""" Minimum time collaborative motions along minium jerk paths"""
#!/usr/bin/env python
import numpy as np

import json
#from optimizer import SafeOptTimeMotionUr3
from modules import gsplines
from modules import gsplinesopt
from gsplinesopt.services.xmlrpc import cGplinesOptXMLRPCServer
from gsplines.services.gsplinesjson import piecewise2json, json2piecewise
from gsplines.functionals.cost1010 import approximate_optimal
import gsplines
#from optimizer import StepInfo
#from plottool import SafeAnimatedSolver
import matplotlib.pyplot as plt
#from diffeotools import identity
import time
#from functionals.derivativecalculator import diff1

from numba import njit
import traceback
import pdb
import functools


def debug_on(*exceptions):
    ''' Decorator for entering in debug mode after exceptions '''
    if not exceptions:
        exceptions = (Exception, )

    def decorator(f):
        @functools.wraps(f)
        def wrapper(*args, **kwargs):
            try:
                return f(*args, **kwargs)
            except exceptions:
                info = sys.exc_info()
                traceback.print_exception(*info)
                pdb.post_mortem(info[2])
                sys.exit(1)

        return wrapper

    return decorator


@njit()
def table_lookup(_t, _time_span, _q_d, _qd_d):
    for i, ti in enumerate(_time_span):
        if ti > _t:
            result_q_d = _q_d[i-1] + (_t - _time_span[i-1]) / \
                (_time_span[i] - _time_span[i-1])*(_q_d[i]-_q_d[i-1])
            result_qd_d = _qd_d[i-1] + (_t - _time_span[i-1]) / \
                (_time_span[i] - _time_span[i-1])*(_qd_d[i]-_qd_d[i-1])
            return result_q_d, result_qd_d


class cMjtServer(cGplinesOptXMLRPCServer):
    def __init__(self, *arg):
        super().__init__(*arg)
        self.kp_ = 0.001
        #self.problem_ = SafeOptTimeMotionUr3(8, 10)
        #        self.problem_.obs_apprx_.gamma_ = 1
        #        self.problem_.obs_apprx_.beta_ = 0.5
        self.qd_d_buff_ = dict()
        self.q_d_buff_ = dict()
        self.time_buff_ = dict()

    @debug_on()
    def control_parameters(self, _kp, _kd, _u_max, eps):
        self.kp_ = _kp
        return True

    #  -- provide to the client the trajectory data

    @debug_on()
    def trajectory_generate(self, _jsonreq):

        json_dict = json.loads(_jsonreq)

        wpvec = np.array(json_dict['waypoints'])
        unique_id = json_dict['unique_id']

        path = approximate_optimal(wpvec, json_dict[
            'regularization_factor']).linear_scaling_new_execution_time(
                json_dict['execution_time'])

        time_span = np.arange(
            0, json_dict['execution_time'] + json_dict['sampling_time'],
            json_dict['sampling_time'])

        max_vel = np.max(np.abs(path.deriv()(time_span)))
        max_acc = np.max(np.abs(path.deriv(2)(time_span)))

        sigma = max(max_vel / json_dict['maximum_speed'],
                    np.sqrt(max_acc / json_dict['maximum_acceleration']))

        execution_time = json_dict['execution_time'] * sigma

        print(execution_time, json_dict['execution_time'])
        path = path.linear_scaling_new_execution_time(execution_time)

        self.trajectories[unique_id] = path
        self.trajectories_deriv_[unique_id] = path.deriv()
        self.time_buff_[unique_id] = time_span
        self.q_d_buff_[unique_id] = path(time_span)
        self.qd_d_buff_[unique_id] = path.deriv()(time_span)

        # Force Numba to compile this funciton
        _, _ = table_lookup(0, self.time_buff_[unique_id],
                            self.q_d_buff_[unique_id],
                            self.qd_d_buff_[unique_id])

        return 'ok'

    @debug_on()
    def trajectory_load(self, unique_id, jsonreq):
        return True

    @debug_on()
    def trajectory_eval(self, unique_id, _t, q_now, qd_now, _scalling=1.0):
        if unique_id not in self.trajectories:
            raise RuntimeError(
                'trajectory {} need to be loaded before evaluating it'.format(
                    unique_id))
        q = self.trajectories[unique_id]
        qd = self.trajectories_deriv_[unique_id]

        t0 = time.time()
        q_d, qd_d = table_lookup(_t, self.time_buff_[unique_id],
                                 self.q_d_buff_[unique_id],
                                 self.qd_d_buff_[unique_id])
        t1 = time.time()

        err = np.array(q_now) - q_d

        err_d = np.array(qd_now) - qd_d

        acc = 1000.0 * float(np.max(np.abs(err_d)))

        u = np.clip(-self.kp_ * err + qd_d, -5, 5)

        result = u.ravel().tolist() + [acc]

        return result

    @debug_on()
    def trajectory_execution_time(self, unique_id):
        """ Evaluates the trajectory unique_id at time t and returns the
        desired joints' position."""
        if unique_id not in self.trajectories:
            raise RuntimeError(
                'trajectory {} need to be loaded before evaluating it'.format(
                    unique_id))
        return float(self.trajectories[unique_id].T_)


if __name__ == '__main__':
    mjt = cMjtServer('/mjt', 5223)
    mjt.serve_forever()

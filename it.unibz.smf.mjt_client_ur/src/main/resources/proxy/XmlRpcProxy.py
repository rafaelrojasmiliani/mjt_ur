#!/usr/bin/env python
from __future__ import print_function
from functools import wraps
from SimpleXMLRPCServer import SimpleXMLRPCServer
from SimpleXMLRPCServer import SimpleXMLRPCRequestHandler
from SocketServer import ThreadingMixIn
import ConfigParser
import json
import numpy as np
import math
import os
import StringIO
import sys
import time
import traceback
from gsplines import json2piecewise
from gsplines import piecewise2json
from gsplines import cSplineCalc
from gsplines import cBasis1010
from gsplines import cBasis0010
import time


def what(function):
    @wraps(function)
    def decorator(*args, **kwargs):
        try:
            return function(*args, **kwargs)
        except Exception as e:
            exc_type, exc_obj, exc_tb = sys.exc_info()
            tb_list = traceback.extract_tb(exc_tb)
            for i, tb in enumerate(tb_list):
                if tb[0].find(__file__) < 0:
                    raise RuntimeError(' "{}" @ {}:{}'.format(
                        str(e), tb_list[i - 1][0], tb_list[i - 1][1]))
            raise RuntimeError(' "{}" @ {}:{}'.format(
                str(e), tb_list[-1][0], tb_list[-1][1]))

    return decorator


class Follower:
    ''' Simple controller for trajectory tracking'''

    def __init__(self, Kp, Kd, u_max, eps):
        self.Kp = Kp
        self.Kd = Kd
        self.u_max = u_max
        self.eps = eps

    def eval(self, _t, trajectory, q_now, qd_now):
        q_now = np.array(q_now)
        qd_now = np.array(q_now)
        q = trajectory(_t)[0]
        qd = trajectory.deriv()(_t)[0]

        qd[qd < self.eps] = 0.0
        qd_now[qd < self.eps] = 0.0

        u = self.Kp*(q-q_now) + self.Kd*(qd - qd_now)

        u = np.clip(u, -self.u_max, self.u_max)

        return u.tolist()


class Planner:
    ''' Trajectory planner example.  This class builds a trajectory.
    '''

    @staticmethod
    def keys():
        return [
            # unique id of the trajectory node, this will be provided by the client
            'unique_id',
            # maximum joint speed that can be attained by any point on the trajectory
            'maximum_speed',
            # maximum joint acceleration that can be attained by any point on the trajectory
            'maximum_acceleration',
            # sampling time for the trajectory evaluation (not used here but on the URScript side)
            'sampling_time',
            'operator_vector',  # ?
            'execution_time',  # total execution time of the trajectory
            'regularization_factor',  # regularization factor between jerk and velocity
            'basis_type',  # a keyword string defining the type of basis
            # list of waypoints, each element of the from: [q[i] for i in range(njoints)]
            'waypoints'
        ]

    def __init__(self, **kwargs):
        diff = set(self.keys()) ^ set(kwargs.keys())
        if len(diff) > 0:
            raise AttributeError(
                'cannot initialize planner: missing or unexpected keys {}'.
                format(list(diff)))
        for key, value in kwargs.items():
            setattr(self, key, value)

        if len(self.waypoints) < 3:
            raise AttributeError(
                'cannot initialize planner: not enough waypoints')

        self.njoints = len(self.waypoints[0])
        for waypoint in self.waypoints:
            if len(waypoint) != self.njoints:
                raise AttributeError(
                    'cannot initialize planner: undefined number of joints')
        if self.njoints < 1:
            raise AttributeError(
                'cannot initialize planner: undefined number of joints')

    def generate(self):
        # trajectory initialization (piece-wise rest-to-rest, same execution time for each polynomial)
        N = len(self.waypoints) - 1
        wp = np.array(self.waypoints)
        tauv = np.array(
            [np.linalg.norm(wp1 - wp2) for wp1, wp2 in zip(wp[:-1], wp[1:])])
        tauv = tauv / np.sum(tauv) * self.execution_time

        splcalc = cSplineCalc(self.njoints, N, cBasis0010())

        trajectory = splcalc.getSpline(tauv, wp)

        del splcalc
        return piecewise2json(trajectory)


class MjtProxy:
    def __init__(self, properties):
        # append dummy section to the java properties file
        config = StringIO.StringIO()
        config.write('[mjt]\n')
        config.write(open(properties).read())
        config.seek(0, os.SEEK_SET)
        # load common java configurations
        cp = ConfigParser.ConfigParser()
        cp.readfp(config)
        self.config = dict(cp.items('mjt'))

        self.trajectories = {}
        self.trajectories_initial_time = {}
        self.follower = None

    # (internal use) custom initialization of the XMLRPC server
    def get_server(self):
        class RequestHandler(SimpleXMLRPCRequestHandler):
            rpc_paths = (self.config['service_rpc_path'], )

        # https://gist.github.com/mcchae/280afebf7e8e4f491a66
        class SimpleThreadXMLRPCServer(ThreadingMixIn, SimpleXMLRPCServer):
            pass

        hostname = self.config['proxy_hostname']
        port = int(self.config['proxy_port_number'])
        server = SimpleThreadXMLRPCServer(
            (hostname, port),
            requestHandler=RequestHandler,
            logRequests=False,
            allow_none=True)
        server.register_instance(self)
        return server

    # proxy service: to obtain from the client trajectory following control parameters, it is mandatory to call this method before trajectory_eval
    @what
    def control_parameters(self, Kp, Kd, u_max, eps):
        self.follower = Follower(Kp, Kd, u_max, eps)
        print('control parameters updated: Kp={}, Kd={}, u_max={}, eps={}'.
              format(Kp, Kd, u_max, eps))
        return True

    # mock of the service provider: to generate a trajectory based on the planning specifications provided by the URCap
    # this method should:
    #  -- compute the trajectory based on the planning specification (provided from the URCap as a JSON request)
    #  -- generate a JSON representation of it (see the Trajectory.to_json())
    #  -- provide to the client the trajectory data
    @what
    def trajectory_generate(self, jsonreq):
        print('planning specification:\n{}'.format(
            json.dumps(json.loads(jsonreq), indent=4)))
        trajectory_json = Planner(**json.loads(jsonreq)).generate()
        return trajectory_json

    # proxy service: to retrieve a trajectory from the service provided and to load it on memory
    @what
    def trajectory_load(self, unique_id, jsonreq):
        print('trajectory specification:\n{}'.format(jsonreq))
        self.trajectories[unique_id] = json2piecewise(jsonreq)
        #        if unique_id != self.trajectories[unique_id].unique_id:
        #            del self.trajectories[unique_id]
        #            raise RuntimeError('trajectory load: {} has a different internal unique_id'.format(unique_id))
        #        print('trajectory load: {}'.format(unique_id))
        return True

    # proxy service: to release a previously loaded trajectory
    @what
    def trajectory_reset(self, unique_id):
        if unique_id not in self.trajectories:
            raise RuntimeError(
                'trajectory {} need to be loaded before resetting it')
        self.trajectories[unique_id].reset()
        self.trajectories_initial_time[unique_id] = time.time()
        print('trajectory reset: {}'.format(unique_id))
        return True

    # proxy service: to evaluate a trajectory and return the corresponding control command to follow it (joint velocities)
    @what
    def trajectory_eval(self, unique_id, q_now, qd_now):
        if not self.follower:
            raise RuntimeError(
                'control parameters need to be defined before evaluating any trajectory'
            )
        if unique_id not in self.trajectories:
            raise RuntimeError(
                'trajectory {} need to be loaded before evaluating it'.format(
                    unique_id))
        current_time = time.time() - self.trajectories_initial_time[unique_id]
        return self.follower.eval(current_time, self.trajectories[unique_id], q_now, qd_now)

    @what
    def trajectory_eval_time(self, unique_id, _t):
        ''' Evaluates the trajectory unique_id at time t and returns the
        desired joints' position.'''
        if unique_id not in self.trajectories:
            raise RuntimeError(
                'trajectory {} need to be loaded before evaluating it'.format(
                    unique_id))
        q = self.trajectories[unique_id](_t)[0]
        return q.tolist()

    @what
    def trajectory_execution_time(self, unique_id):
        ''' Evaluates the trajectory unique_id at time t and returns the
        desired joints' position.'''
        if unique_id not in self.trajectories:
            raise RuntimeError(
                'trajectory {} need to be loaded before evaluating it'.format(
                    unique_id))
        return float(self.trajectories[unique_id].T_)


if __name__ == '__main__':
    os.chdir(sys.path[0])
    mjt = MjtProxy('../urcap.properties')
    server = mjt.get_server()
    server.register_introspection_functions()
    server.serve_forever()

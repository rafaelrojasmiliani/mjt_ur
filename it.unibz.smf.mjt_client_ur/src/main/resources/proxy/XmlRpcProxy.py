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
                    raise RuntimeError(' "{}" @ {}:{}'.format(str(e), tb_list[i-1][0], tb_list[i-1][1]))
            raise RuntimeError(' "{}" @ {}:{}'.format(str(e), tb_list[-1][0], tb_list[-1][1]))
    return decorator


# doubly normalized polynomial
class Polynomial:
    @staticmethod
    def keys():
        return [
            'to',        # start time of the polynomial
            'tf',        # end time of the polynomial
            'xo',        # initial conditions at t=to: [[q[i](to), qp[i](to), qpp(to)] for i in range(njoints)]
            'xf',        #   final conditions at t=tf: [[q[i](tf), qp[i](tf), qpp(tf)] for i in range(njoints)]
            'basis_type' # a keyword string defining the type of basis
        ]

    def generate(self):
        if self.basis_type == 'quintic_polynomial':
            def time_scale(t, to, tf):
              return (t - to) / (tf - to)
            def bases(t):
              t1 = t
              t2 = t1 * t1
              t3 = t1 * t2
              t4 = t1 * t3
              t5 = t1 * t4
              return [
                  [   t5,    t4,   t3,   t2, t1, 1], # position
                  [ 5*t4,  4*t3, 3*t2, 2*t1,  1, 0], # velocity
                  [20*t3, 12*t2, 6*t1,    2,  0, 0]  # acceleration
              ]
            self.order = 5
            self.time_scale = time_scale
            self.bases = bases

        else:
            raise AttributeError('unknown basis functions type \'{}\''.format(basis_type))

        self.qo = self.njoints * [0]
        self.dq = self.njoints * [0]
        self.coefficients = []
        B = np.linalg.inv(np.array(self.bases(0) + self.bases(1)))
        for i in range(self.njoints):
            # normalization of initial and final conditions
            self.qo[i] = self.xo[i][0]
            self.dq[i] = self.xf[i][0] - self.xo[i][0]
            if abs(self.dq[i]) > 1e-3:
                xo = [0, self.xo[i][1] * self.dt / self.dq[i], self.xo[i][2] * self.dt2 / self.dq[i]]
                xf = [1, self.xf[i][1] * self.dt / self.dq[i], self.xf[i][2] * self.dt2 / self.dq[i]]
            else:
                xo = [0, 0, 0]
                xf = [1, 0, 0]
            # coefficients computation
            self.coefficients.append(B.dot(np.array(xo + xf)).tolist())

    def __init__(self, **kwargs):
        diff = set(self.keys()) ^ set(kwargs.keys())
        if len(diff) > 0:
            raise AttributeError('cannot initialize polynomial: missing or unexpected keys {}'.format(list(diff)))
        for key, value in kwargs.items():
            setattr(self, key, value)

        if self.to >= self.tf:
            raise AttributeError('cannot initialize polynomial: invalid time interval')
        self.dt = self.tf - self.to
        self.dt2 = self.dt * self.dt

        self.njoints = len(self.xo)
        for i in range(self.njoints):
            if len(self.xo[i]) != 3 or len(self.xo[i]) != len(self.xf[i]):
                raise AttributeError('cannot initialize polynomial: malformed boundary conditions')
        if self.njoints < 1:
            raise AttributeError('cannot initialize polynomial: invalid number of joinst')

        self.generate()

    def check(self, t):
        return t >= self.to and t <= self.tf

    def eval(self, t, q, qp, qpp):
        # evaluation of a doubly normalized polynomial
        B = self.bases(self.time_scale(t, self.to, self.tf))
        for i, c in enumerate(self.coefficients):
            q[i] = 0
            qp[i] = 0
            qpp[i] = 0
            for j in range(len(c)):
                q[i] = q[i] + B[0][j] * c[j]
                qp[i] = qp[i] + B[1][j] * c[j]
                qpp[i] = qpp[i] + B[2][j] * c[j]
            q[i] = self.qo[i] + self.dq[i] * q[i]
            qp[i] = self.dq[i] * qp[i] / self.dt
            qpp[i] = self.dq[i] * qpp[i] / self.dt2


# trajectory container
class Trajectory:
    @staticmethod
    def keys():
        return [
            'unique_id',      # unique id of the trajectory node, this will be provided by the client inside the planning specification
            'sampling_time',  # sampling time for the trajectory evaluation (not used here but on the URScript side)
            'execution_time', # total execution time of the trajectory
            'njoints',        # dimension of the joint space
            'polynomials'     # list of polynomial specifications (i.e., a list of dictionaries with the constructor arguments)
        ]

    def __init__(self, **kwargs):
        diff = set(self.keys()) ^ set(kwargs.keys())
        if len(diff) > 0:
            raise AttributeError('cannot initialize trajectory: missing or unexpected keys {}'.format(list(diff)))
        for key, value in kwargs.items():
            if key == 'polynomials':
                setattr(self, key, [poly if isinstance(poly, Polynomial) else Polynomial(**poly) for poly in value])
            else:
                setattr(self, key, value)
        self.reset()

    def eval(self):
        if math.isnan(self.start_time):
            self.start_time = time.time()
            self.current_time = 0.0
        else:
            time_now = time.time()
            self.current_time = time_now - self.start_time

        for poly in self.polynomials:
            if poly.check(self.current_time):
                poly.eval(self.current_time, self.q, self.qp, self.qpp)
                return

        # time beyond trajectory domain
        self.polynomials[-1].eval(self.polynomials[-1].tf, self.q, self.qp, self.qpp)

    def reset(self):
        self.start_time = float('NaN')
        self.current_time = float('NaN')
        self.q = self.njoints * [0.0]
        self.qp = self.njoints * [0.0]
        self.qpp = self.njoints * [0.0]
        self.u = self.njoints * [0.0]

    def to_json(self):
        trajectory = dict(self.__dict__)
        for key in set(trajectory.keys()) ^ set(self.keys()):
            del trajectory[key]
        trajectory['polynomials'] = []
        for poly in self.__dict__['polynomials']:
            poly = dict(poly.__dict__)
            for key in set(poly.keys()) ^ set(Polynomial.keys()):
                del poly[key]
            trajectory['polynomials'].append(poly)
        return json.dumps(trajectory)


class Follower:
    def __init__(self, Kp, Kd, u_max, eps):
        self.Kp = Kp
        self.Kd = Kd
        self.u_max = u_max
        self.eps = eps

    def eval(self, trajectory, q_now, qp_now):
        q = trajectory.q
        qp = trajectory.qp
        qpp = trajectory.qpp
        u = trajectory.u

        u_max = 0
        for i in range(len(q)):
            u[i] = self.Kp * (q[i] - q_now[i]) + (self.Kd * (qp[i] - qp_now[i]) if abs(qp[i]) > self.eps else 0.0)
            u_abs = abs(u[i])
            u_max = u_abs if u_abs > u_max else u_max
        if u_max > self.u_max:
            for i in range(len(q)):
                u[i] = u[i] * self.u_max / u_max

        if math.sqrt(sum([e*e for e in u])) < self.eps and trajectory.current_time >= trajectory.execution_time:
            return len(u) * [float('nan')]

        #print('{:.2f} {:.2f} {:.2f} {:.2f} {:.2f} {:.2f}   {:.2f} {:.2f} {:.2f} {:.2f} {:.2f} {:.2f}   {:.2f} {:.2f} {:.2f} {:.2f} {:.2f} {:.2f}'.format(*(q + q_now + u)))
        return u


# mock of the service provider planner
class Planner:
    @staticmethod
    def keys():
        return [
            'unique_id',             # unique id of the trajectory node, this will be provided by the client
            'maximum_speed',         # maximum joint speed that can be attained by any point on the trajectory
            'maximum_acceleration',  # maximum joint acceleration that can be attained by any point on the trajectory
            'sampling_time',         # sampling time for the trajectory evaluation (not used here but on the URScript side)
            'operator_vector',       # ?
            'execution_time',        # total execution time of the trajectory
            'regularization_factor', # regularization factor between jerk and velocity
            'basis_type',            # a keyword string defining the type of basis
            'waypoints'              # list of waypoints, each element of the from: [q[i] for i in range(njoints)]
        ]

    def __init__(self, **kwargs):
        diff = set(self.keys()) ^ set(kwargs.keys())
        if len(diff) > 0:
            raise AttributeError('cannot initialize planner: missing or unexpected keys {}'.format(list(diff)))
        for key, value in kwargs.items():
            setattr(self, key, value)

        if len(self.waypoints) < 3:
            raise AttributeError('cannot initialize planner: not enough waypoints')

        self.njoints = len(self.waypoints[0])
        for waypoint in self.waypoints:
            if len(waypoint) != self.njoints:
                raise AttributeError('cannot initialize planner: undefined number of joints')
        if self.njoints < 1:
            raise AttributeError('cannot initialize planner: undefined number of joints')

    def polyargs(self, dt, i):
        xo = [[self.waypoints[i][j], 0, 0]  for j in range(self.njoints)]
        xf = [[self.waypoints[i + 1][j], 0, 0]  for j in range(self.njoints)]
        return {'xo': xo, 'xf': xf, 'to': dt * i, 'tf': dt * (i + 1), 'basis_type': self.basis_type}

    def generate(self):
        # trajectory initialization (piece-wise rest-to-rest, same execution time for each polynomial)
        npolynomials = len(self.waypoints) - 1
        dt = self.execution_time / npolynomials
        self.polynomials = [Polynomial(**self.polyargs(dt, i)) for i in range(npolynomials)]

        # execution times optimization
        #self.optimize()

        # return a trajectory container
        kwargs = {key: getattr(self, key) for key in Trajectory.keys()}
        del self.polynomials
        return Trajectory(**kwargs)


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
        self.follower = None

    # (internal use) custom initialization of the XMLRPC server
    def get_server(self):
        class RequestHandler(SimpleXMLRPCRequestHandler):
            rpc_paths = (self.config['service_rpc_path'],)

        #https://gist.github.com/mcchae/280afebf7e8e4f491a66
        class SimpleThreadXMLRPCServer(ThreadingMixIn, SimpleXMLRPCServer):
            pass

        hostname = self.config['proxy_hostname']
        port = int(self.config['proxy_port_number'])
        server = SimpleThreadXMLRPCServer((hostname, port), requestHandler=RequestHandler, logRequests=False, allow_none=True)
        server.register_instance(self)
        return server

    # proxy service: to obtain from the client trajectory following control parameters, it is mandatory to call this method before trajectory_eval
    @what
    def control_parameters(self, Kp, Kd, u_max, eps):
        self.follower = Follower(Kp, Kd, u_max, eps)
        print('control parameters updated: Kp={}, Kd={}, u_max={}, eps={}'.format(Kp, Kd, u_max, eps))
        return True

    # mock of the service provider: to generate a trajectory based on the planning specifications provided by the URCap
    # this method should:
    #  -- compute the trajectory based on the planning specification (provided from the URCap as a JSON request)
    #  -- generate a JSON representation of it (see the Trajectory.to_json())
    #  -- provide to the client the trajectory data
    @what
    def trajectory_generate(self, jsonreq):
        print('planning specification:\n{}'.format(json.dumps(json.loads(jsonreq), indent=4)))
        trajectory = Planner(**json.loads(jsonreq)).generate()
        print('trajectory generate: {}'.format(trajectory.unique_id))
        return trajectory.to_json()

    # proxy service: to retrieve a trajectory from the service provided and to load it on memory
    @what
    def trajectory_load(self, unique_id, jsonreq):
        print('trajectory specification:\n{}'.format(jsonreq))
        self.trajectories[unique_id] = Trajectory(**json.loads(jsonreq))
        if unique_id != self.trajectories[unique_id].unique_id:
            del self.trajectories[unique_id]
            raise RuntimeError('trajectory load: {} has a different internal unique_id'.format(unique_id))
        print('trajectory load: {}'.format(unique_id))
        return True

    # proxy service: to release a previously loaded trajectory
    @what
    def trajectory_reset(self, unique_id):
        if unique_id not in self.trajectories:
            raise RuntimeError('trajectory {} need to be loaded before resetting it')
        self.trajectories[unique_id].reset()
        print('trajectory reset: {}'.format(unique_id))
        return True

    # proxy service: to evaluate a trajectory and return the corresponding control command to follow it (joint velocities)
    @what
    def trajectory_eval(self, unique_id, q_now, qp_now):
        if not self.follower:
            raise RuntimeError('control parameters need to be defined before evaluating any trajectory')
        if unique_id not in self.trajectories:
            raise RuntimeError('trajectory {} need to be loaded before evaluating it'.format(unique_id))
        self.trajectories[unique_id].eval()
        return self.follower.eval(self.trajectories[unique_id], q_now, qp_now)


if __name__ == '__main__':
    os.chdir(sys.path[0])
    mjt = MjtProxy('../urcap.properties')
    server = mjt.get_server()
    server.register_introspection_functions()
    server.serve_forever()

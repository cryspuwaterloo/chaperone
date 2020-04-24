import numpy as np


class Kalman1DAcc:
    def add(self, ob):
        xm = self.F @ self.hat_x[-1]
        pm = self.F @ self.hat_p[-1] @ self.F.T + self.Q
        # correction
        K = pm @ self.H.T @ np.linalg.inv(self.H @ pm @ self.H.T + self.R)
        xt = xm + K @ (ob - self.H @ xm)
        pt = (np.eye(2) - K @ self.H) @ pm
        self.hat_x.append(xt)
        self.hat_p.append(pt)
        return xt

    def __init__(self, t, Q=1e-2, R=0.01, initX0=0.0, initP=1.0):
        self.dt = t
        self.F = np.array([[1, self.dt], [0, 1]])
        self.G = np.array([[(self.dt ** 2)/2], [self.dt]])
        self.H = np.array([[1, 0]]) # np.eye(2)
        self.x0 = np.array([[initX0], [0]])
        self.Q = Q * np.array([[1, 0], [0, 1]])
        self.R = np.array([[R]]) # * np.eye(2)
        self.hat_x = [self.x0]
        self.hat_p = [initP * np.eye(2)]
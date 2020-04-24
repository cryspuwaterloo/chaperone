import numpy as np
from ASAnalyzer.logger import logger

def dist_to_index(dist, cols, ceil=False):
    if ceil:
        return int(np.where(cols >= dist)[0][0])
    else:
        return int(np.where(cols <= dist)[0][-1])


def range_to_index(ran, cols):
    # x0, x1 -> index_0, index_1
    assert(len(ran) == 2)
    x0 = ran[0]
    x1 = ran[1]
    return dist_to_index(x0, cols, True), dist_to_index(x1, cols, True)


def select_dist(row, cols, x0=0, x1=None):
    # return the magnitude data of selected distances
    # not recommended
    if x1 is None:
        return row[cols >= x0], cols[cols >= x0]
    else:
        return row[(cols >= x0) & (cols < x1)], \
               cols[(cols >= x0) & (cols < x1)]


def abs_diff(data):
    return np.abs(np.diff(data, axis=0))


def abs_subt(curr, prev):
    return np.abs(curr - prev)


def positive_subt(curr, prev):
    tmp = curr - prev
    tmp[tmp < 0] = 0
    return tmp


# ======================================
# Detection functions
# ======================================

def median_outlier_index(row, m):
    # return outlier's index as well as the data
    d = np.abs(row - np.median(row))
    mdev = np.median(d)
    s = d / mdev if mdev else 0.
    a = np.where(s > m)
    return a[0]


def percentile_outlier_index(row, p):
    num = np.percentile(row, p)
    return np.where(row > num)[0]

def median_outlier_rejection(row, m):
    # return the data array without outliers
    d = np.abs(row - np.median(row))
    mdev = np.median(d)
    s = d / mdev if mdev else 0.
    return row[s<m]


def ambient_upper_bound(data, m):
    # intuition: first reject the outliers, and then select the max value
    median = np.average(data, axis=1)
    upper_bound = np.max(median_outlier_rejection(median, m))
    logger.info('[Process] ambient sound magnitude is estimated as {} based '
                'on {} time slots '
                .format(upper_bound, data.shape[0]))
    return upper_bound

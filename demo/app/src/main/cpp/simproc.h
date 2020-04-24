//
// Created by Jiayi Chen on 2019-03-13.
// simproc is a simplified version of preprocessing module
// aiming a lower latency when processing raw data
//

#ifndef CHAPERONEPROJECT_SIMPROC_H
#define CHAPERONEPROJECT_SIMPROC_H

#include "utils.h"

void rawToMagnitude(float *raw, int rawSize, int startIdx, int endIdx, float *dest, int destSize);
void makeConvolveInDestSize(float *source, int sourceSize, float *filter, int filterSize, float *dest, int destSize, bool returnAbs);



#endif //CHAPERONEPROJECT_SIMPROC_H

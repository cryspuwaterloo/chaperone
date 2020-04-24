//
// Created by Jiayi Chen on 2019-03-13.
// The logging code is from LibAS Example
//

#ifndef CHAPERONEPROJECT_UTILS_H
#define CHAPERONEPROJECT_UTILS_H

#include <jni.h>
#include <android/log.h>
#include <vector>
#include <assert.h>
#include <string.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

#define DEBUG_TAG "Chaperone-NDK"

#define SHOW_DEBUG_MESSAGE true

#define DEBUG_MACRO(x) __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "%s", x);
#define DEBUG_MACRO_WARN(x) __android_log_print(ANDROID_LOG_WARN, DEBUG_TAG, "[WARN]: %s", x);
#define DEBUG_MACRO_ERROR(x) __android_log_print(ANDROID_LOG_ERROR, DEBUG_TAG, "[ERROR]: %s", x);
#define DEBUG_MACRO_ASSERT(x) __android_log_print(ANDROID_LOG_ERROR, DEBUG_TAG, "[ASSERT FAIL]: %s", x);
#define DEBUG_STRING_BUFFER_SIZE 1024

void debug(const char *s,...);
void warn(const char *s,...);
void error(const char *s,...);

#endif //CHAPERONEPROJECT_UTILS_H

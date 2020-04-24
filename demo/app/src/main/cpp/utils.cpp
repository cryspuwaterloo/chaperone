//
// Created by Jiayi Chen on 2019-03-13.
//

#include "utils.h"

// Some copied utility functions
void debug(const char *s,...)
{
    if(SHOW_DEBUG_MESSAGE){
        va_list va; va_start(va,s);
#ifdef DEV_NDK
        char buffer[DEBUG_STRING_BUFFER_SIZE];
        vsprintf(buffer,s,va);
        va_end(va);
        DEBUG_MACRO(buffer);
#else
        // ref: http://stackoverflow.com/questions/8924831/iphone-debugging-real-device
        //NSLogv([NSString stringWithUTF8String:s], va); // dosn't work on c++
        printf("NDK: ");
        vprintf(s, va);
        printf("\n"); // automaticlaly change the line for visualzation
        va_end(va);
#endif
    }
}

void warn(const char *s,...)
{
    va_list va; va_start(va,s);
#ifdef DEV_NDK
    char buffer[DEBUG_STRING_BUFFER_SIZE];
    vsprintf(buffer,s,va);
    va_end(va);
    DEBUG_MACRO_WARN(buffer);
#else
    printf("NDK [WARN]: ");
    vprintf(s, va);
    printf("\n"); // automaticlaly change the line for visualzation
    va_end(va);
#endif
}

void error(const char *s,...)
{
    va_list va; va_start(va,s);
#ifdef DEV_NDK
    char buffer[DEBUG_STRING_BUFFER_SIZE];
    vsprintf(buffer,s,va);
    va_end(va);
    DEBUG_MACRO_ERROR(buffer);
#else
    printf("NDK [ERROR]: ");
    vprintf(s, va);
    printf("\n"); // automaticlaly change the line for visualzation
    va_end(va);
#endif
}
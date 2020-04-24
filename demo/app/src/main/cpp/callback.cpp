#include "callback.h"
#include <ctime>


extern "C" void JNI_FUNC_NAME(debugTest)(JNI_FUNC_NO_PARAM) {
    debug("JNI is correctly imported");
}


static double now_ms(void) {

    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;

}


struct AddAudioRet {
    int chCnt;
    int traceCnt;
    int sampleCnt;
    float ***data;
};

#define AUDIO_CH_CNT_MAX 2
#define AUDIO_REPEAT_CNT 500
#define PULSE_DETECTION_MAX_RANGE_SAMPLES (2400)
float gConBufs[AUDIO_CH_CNT_MAX][AUDIO_REPEAT_CNT][PULSE_DETECTION_MAX_RANGE_SAMPLES];


extern "C" jfloatArray JNI_FUNC_NAME(preprocessEmulator)(JNI_FUNC_PARAM jfloatArray data) {
    // this function aims to test if the sample input data can give the correct results
    float* cData = env->GetFloatArrayElements(data, 0);

    debug("goto processing module");
    float result[300];
    rawToMagnitude(&cData[0], 2400, 600, 1800, &result[0], 300);
    debug("new result = [%.2f, %.2f]", result[10], result[100]);
    jfloatArray resultObj = env->NewFloatArray(300);
    env->SetFloatArrayRegion(resultObj, 0, 300, result);
    return resultObj;
}


extern "C" jfloatArray JNI_FUNC_NAME(dataCallback)(JNI_FUNC_PARAM jlong retAddr) {
    AddAudioRet *r = (AddAudioRet *)retAddr;
    debug("retAddr (jlong) = %ld", retAddr);

    float orgData[2400];


    memcpy(&orgData[0], *(*(r->data + 0) + 1), 2400 * sizeof(float));

    double t0 = now_ms();
    float result[300];
    /*
    Preprocessing(orgData, SIGNAL_CORRELATE, result);
    */
    rawToMagnitude(&orgData[0], 2400, 600, 1800, &result[0], 300);
    debug("time for pre-processing: %f ms", now_ms() -t0);
    // debug("new result = [%.2f, %.2f]", result[0], result[100]);
    jfloatArray resultObj = env->NewFloatArray(300);
    env->SetFloatArrayRegion(resultObj, 0, 300, result);

    return resultObj;
}


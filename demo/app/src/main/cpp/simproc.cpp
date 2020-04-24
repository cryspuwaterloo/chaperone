//
// Created by Jiayi Chen on 2019-03-13.
//

#include "simproc.h"

float SIGNAL_CORRELATE[] = {-0,0.00014961,-0.00054785,0.0010969,-0.0016738,0.0021364,-0.0023304,0.0020978,
                            -0.0012853,-0.00024637,0.0026138,-0.0059016,0.010155,-0.015373,0.021503,-0.028436,
                            0.036008,-0.043995,0.052124,-0.060072,0.067475,-0.073941,0.07906,-0.08242,0.083618,
                            -0.082284,0.078091,-0.070779,0.060163,-0.046155,0.028775,-0.0081584,-0.015433,0.041612,
                            -0.069861,0.099543,-0.12991,0.16011,-0.18923,0.21629,-0.24027,0.26019,-0.27506,0.284,
                            -0.28621,0.28105,-0.26802,0.24685,-0.21748,0.18009,-0.13512,0.083288,-0.025543,-0.036902,
                            0.10261,-0.16994,0.2371,-0.30218,0.36319,-0.41815,0.46512,-0.50229,0.52801,-0.5409,0.53986,
                            -0.52415,0.49343,-0.44778,0.38774,-0.31431,0.22895,-0.13354,0.030389,0.077879,-0.18834,
                            0.29786,-0.4032,0.50109,-0.58834,0.66194,-0.71919,0.75776,-0.7758,0.77203,-0.74578,0.69708,
                            -0.62664,0.53587,-0.4269,0.30249,-0.16596,0.021179,0.12764,-0.276,0.41927,-0.55285,0.67232,
                            -0.77358,0.85299,-0.90754,0.93495,-0.93375,0.90338,-0.84425,0.75771,-0.64605,0.51247,-0.36094,
                            0.19617,-0.023348,-0.15191,0.32383,-0.48663,0.63475,-0.76303,0.86693,-0.94265,0.98735,-0.99922,
                            0.97756,-0.92287,0.8371,-0.7229,0.58396,-0.42483,0.2508,-0.067745,-0.11807,0.30022,-0.47234,
                            0.62832,-0.76257,0.8702,-0.94719,0.9906,-0.99863,0.97075,-0.90772,0.81159,-0.68564,0.53429,
                            -0.36293,0.17777,0.01441,-0.20648,0.39124,-0.56168,0.71126,-0.83416,0.9255,-0.98158,0.99999,
                            -0.97978,0.92148,-0.82711,0.70012,-0.54532,0.36862,-0.17691,-0.02227,0.22098,-0.41124,0.58529,
                            -0.73597,0.85699,-0.9432,0.99084,-0.99768,0.96319,-0.88854,0.77658,-0.63179,0.46007,-0.26851,
                            0.065131,0.14145,-0.3424,0.52902,-0.69315,0.82754,-0.92612,0.98436,-0.99945,0.97046,-0.89839,
                            0.78619,-0.63862,0.4621,-0.2644,0.054342,0.15861,-0.36476,0.55462,-0.71935,0.8512,-0.94385,0.99276,
                            -0.99537,0.95128,-0.86229,0.73234,-0.56736,0.375,-0.16432,-0.054669,0.27145,-0.47551,0.65684,
                            -0.80646,0.91684,-0.98232,0.9994,-0.96696,0.88632,-0.76123,0.5977,-0.40376,0.18904,0.035692,
                            -0.25903,0.46954,-0.65634,0.80967,-0.9214,0.98548,-0.9983,0.95891,-0.86907,0.73323,-0.55834,
                            0.35345,-0.12934,-0.10212,0.32851,-0.53761,0.71798,-0.85968,0.95475,-0.99773,0.98597,-0.91982,
                            0.80263,-0.64064,0.44263,-0.21949,-0.016375,0.25175,-0.4733,0.66837,-0.82569,0.93607,-0.9929,
                            0.99263,-0.93495,0.82291,-0.66275,0.46356,-0.23682,-0.0042578,0.24551,-0.47262,0.67201,-0.83162,
                            0.94167,-0.99527,0.9889,-0.92262,0.80015,-0.62866,0.41839,-0.18206,-0.065893,0.3102,-0.53567,
                            0.72816,-0.87547,0.96812,-1,0.96878,-0.8761,0.72757,-0.53235,0.30272,-0.053252,-0.20007,0.44087,
                            -0.65346,0.82384,-0.94067,0.99602,-0.98592,0.91072,-0.77507,0.58768,-0.36079,0.10939,0.14977,-0.39926,
                            0.62218,-0.80328,0.93002,-0.99349,0.98901,-0.91657,0.78083,-0.59085,0.35957,-0.10288,-0.16141,0.41482,
                            -0.63946,0.81936,-0.94156,0.9971,-0.9817,0.89612,-0.74616,0.54229,-0.29897,0.033619,0.23459,-0.48613,
                            0.70254,-0.86777,0.96942,-0.9997,0.95601,-0.84125,0.66364,-0.43616,0.17562,0.098533,-0.36567,0.60555,
                            -0.79982,0.93347,-0.99601,0.9823,-0.89305,0.73479,-0.51944,0.26346,0.013428,-0.28968,0.54367,-0.75532,
                            0.90776,-0.98867,0.9913,-0.91508,0.76574,-0.55489,0.29918,-0.018995,-0.26314,0.52438,-0.74339,0.90214,
                            -0.98738,0.99177,-0.9146,0.76186,-0.54587,0.28425,0.0014193,-0.28738,0.5497,-0.76623,0.91853,-0.99344,
                            0.98427,-0.89142,0.72245,-0.49147,0.218,0.074606,-0.3612,0.61696,-0.81955,0.95111,-0.99985,0.96114,
                            -0.83799,0.64089,-0.38692,0.098316,0.19942,-0.47982,0.71776,-0.89172,0.98581,-0.9912,0.90703,-0.74054,
                            0.50651,-0.22599,-0.075585,0.37065,-0.63205,0.83554,-0.96207,0.99957,-0.94418,0.80067,-0.5821,0.30864,
                            -0.0057862,-0.29803,0.57408,-0.79607,0.94266,-0.99955,0.96093,-0.8301,0.61927,-0.34844,0.043547,0.26598,
                            -0.55007,0.7809,-0.93568,0.99894,-0.96406,0.8341,-0.6215,0.34701,-0.037656,-0.27586,0.56222,-0.79262,0.94367,
                            -0.99983,0.95504,-0.81343,0.589,-0.30428,-0.0119,0.32727,-0.60945,0.82925,-0.96372,0.99859,-0.92986,0.76426,
                            -0.5187,0.21854,0.10494,-0.4178,0.68699,-0.88388,0.98731,-0.9859,0.8794,-0.67878,0.40516,-0.087663,-0.23968,
                            0.54156,-0.78517,0.94385,-0.99999,0.94705,-0.79042,0.54687,-0.24286,-0.088316,0.41014,-0.68691,0.88768,
                            -0.98979,0.98145,-0.86317,0.64782,-0.35926,0.0298,0.30345,-0.60268,0.83374,-0.97001,0.99555,-0.90703,
                            0.71417,-0.43881,0.11243,0.22737,-0.54119,0.79242,-0.95151,0.99952,-0.93038,0.7518,-0.48441,0.15947,
                            0.18475,-0.50745,0.77015,-0.94126,0.99999,-0.93888,0.76483,-0.49839,0.17143,0.17669,-0.50378,0.76994,
                            -0.94247,0.99994,-0.93487,0.75482,-0.48155,0.14847,0.20338,-0.53041,0.79182,-0.95478,0.99856,-0.91723,
                            0.72056,-0.43291,0.090273,0.26419,-0.58556,0.83283,-0.97423,0.9913,-0.88142,0.65824,-0.35008,-0.0036027,
                            0.35723,-0.66495,0.88662,-0.99307,0.96998,-0.81992,0.56213,-0.23013,-0.13258,0.47819,-0.76087,0.94287,
                            -0.99963,0.92313,-0.72313,0.42601,-0.071339,-0.29334,0.61885,-0.86102,0.98673,-0.9785,0.83698,-0.58103,
                            0.2453,0.12446,-0.47752,0.76518,-0.94747,0.99881,-0.91162,0.69755,-0.38601,0.020196,0.34885,-0.66934,
                            0.89603,-0.99662,0.95649,-0.78083,0.49413,-0.1368,-0.24042,0.5837,-0.84372,0.98289,-0.98075,0.83716,
                            -0.57238,0.22439,0.15656,-0.51514,0.79897,-0.96629,0.99221,-0.87245,0.62415,-0.28351,-0.099402,0.468,
                            -0.76749,0.95308,-0.9967,0.89137,-0.65238,0.31518,0.069815,-0.44469,0.75274,-0.94709,0.99786,-0.89685,
                            0.65898,-0.32015,-0.068072,0.44625,-0.75618,0.94986,-0.99698,0.88978,-0.6444,0.29855,0.094187,-0.47262,
                            0.77748,-0.96068,0.99306,-0.86901,0.60763,-0.24984,-0.14793,0.52261,-0.81445,0.97661,-0.98275,0.83138,
                            -0.54632,0.17304,0.22854,-0.59358,0.86284,-0.99231,0.96047,-0.77202,0.45726,-0.067309,-0.33407,0.68094,
                            -0.91596,0.99999,-0.91866,0.68493,-0.33716,-0.067092,0.46055,-0.77741,0.96433,-0.98952,0.84826,-0.56385,
                            0.18389,0.22758,-0.60085,0.87234,-0.99549,0.94883,-0.7398,0.40376,0.0019651,-0.40775,0.74361,-0.95128,
                            0.9944,-0.86499,0.58503,-0.20285,-0.21512,0.59586,-0.8725,0.99612,-0.9445,0.72622,-0.37935,-0.035038,
                            0.44361,-0.77369,0.96621,-0.98639,0.8301,-0.52484,0.125,0.29772,-0.66715,0.91636,-0.99985,0.902,-0.64005,
                            0.26124,0.16561,-0.56259,0.85682,-0.99399,0.94838,-0.72786,0.37268,0.051834,-0.46713,0.796,-0.97696,0.97581,
                            -0.79222,0.46007,-0.041256,-0.38571,0.74039,-0.95562,0.99028,-0.83728,0.52512,-0.11275,-0.32149,0.69473,
                            -0.93534,0.9968,-0.86679,0.56978,-0.1626,-0.27639,0.66225,-0.91999,0.99912,-0.88373,0.59578,-0.19107,
                            -0.25143,0.64499,-0.91207,0.99968,-0.89003,0.60425,-0.19846,-0.24709,0.6439,-0.91274,0.99955,-0.88647,
                            0.5956,-0.18485,-0.26346,0.65906,-0.9219,0.99843,-0.87261,0.56943,-0.15009,-0.30022,0.6896,-0.9382,0.99465,
                            -0.84682,0.52456,-0.09386,-0.35661,0.73368,-0.95894,0.98518,-0.80639,0.45929,-0.015939,-0.43114,0.78821,
                            -0.98,0.9657,-0.74776,0.37166,0.083421,-0.52121,0.84867,-0.9958,0.93074,-0.66682,0.25998,0.20295,-0.6227,
                            0.90881,-0.99927,0.87399,-0.55951,0.12349,0.33973,-0.72943,0.9605,-0.98205,0.78881,-0.42255,-0.036783,0.48842,
                            -0.83283,0.99371,-0.93499,0.6691,-0.25449,-0.21704,0.64064,-0.92165,0.99689,-0.84896,0.51046,-0.056958,
                            -0.40975,0.78443,-0.9822,0.95782,-0.71623,0.31176,0.164,-0.60277,0.90425,-0.99912,0.8651,-0.53235,0.076892,
                            0.39666,-0.77905,0.98164,-0.95703,0.71034,-0.29834,-0.18346,0.62287,-0.9171,0.9969,-0.84302,0.49099,
                            -0.023143,-0.45054,0.81823,-0.99268,0.93205,-0.65015,0.21342,0.2745,-0.69731,0.9538,-0.98213,0.77493,
                            -0.38137,-0.10429,0.5652,-0.88998,0.99972,-0.86728,0.52419,-0.053361,-0.43085,0.81031,-0.99202,0.93098,
                            -0.64156,0.19429,0.30116,-0.72275,0.96621,-0.97085,0.73494,-0.31642,-0.1811,0.63391,-0.92893,0.99206,
                            -0.80691,0.41928,0.073952,-0.54897,0.88602,-0.99967,0.86063,-0.50349,0.01834,0.47185,-0.84231,0.99828,
                            -0.89939,0.57032,-0.094839,-0.40536,0.80172,-0.99189,0.92633,-0.62133,0.15527,0.35141,-0.76721,0.98376,
                            -0.94414,0.65807,-0.19975,-0.31124,0.74098,-0.97637,0.95501,-0.6819,0.22854,0.28561,-0.72449,0.97148,
                            -0.96047,0.69378,-0.24191,-0.27492,0.71859,-0.97006,0.96138,-0.69426,0.24,0.27932,-0.72358,0.9724,-0.95788,
                            0.68333,-0.22279,-0.29876,0.73922,-0.97804,0.94941,-0.66054,0.19011,0.33294,-0.76469,0.98581,-0.93472,
                            0.62492,-0.14167,-0.38127,0.79857,-0.99377,0.91189,-0.57516,0.077218,0.44273,-0.83877,0.99924,-0.87841,
                            0.50971,0.0032752,-0.51571,0.88235,-0.99878,0.83132,-0.427,-0.099402,0.59779,-0.9255,0.98823,-0.76735,
                            0.32573,0.21,-0.68556,0.96345,-0.96284,0.68325,-0.2052,-0.33284,0.77438,-0.99046,0.91749,-0.57614,
                            0.065784,0.46423,-0.85823,0.99995,-0.84705,0.444,0.090599,-0.59875,0.9297,-0.98474,0.74689,-0.28634,
                            -0.25998,0.72899,-0.98013,0.93763,-0.6136,0.10483,0.43586,-0.84548,0.99998,-0.85206,0.44586,0.095925,
                            -0.60893,0.93695,-0.97956,0.72313,-0.2454,-0.30781,0.767,-0.99091,0.91013,-0.54888,0.018013,0.51879,
                            -0.8954,0.99472,-0.78531,0.3316,0.22567,-0.71295,0.97791,-0.93718,0.60285,-0.079177,-0.46974,0.87122,
                            -0.99843,0.81063,-0.36649,-0.19397,0.69331,-0.97288,0.94331,-0.61334,0.087555,0.46655,-0.87181,0.99808,
                            -0.80425,0.35192,0.21395,-0.71126,0.97939,-0.93118,0.58157,-0.043219,-0.50952,0.89699,-0.99271,0.76483,
                            -0.28707,-0.28487,0.76391,-0.99275,0.89569,-0.50397,-0.054015,0.59455,-0.93903,0.97308,-0.68477,0.16906,
                            0.40316,-0.84161,0.99988,-0.82458,0.37359,0.20274,-0.71149,0.98176,-0.9222,0.55216,0.0042578,-0.5596,
                            0.92612,-0.97932,0.70051,-0.18367,-0.39596,0.84114,-0.99977,0.8171,-0.35488,-0.22918,0.73501,-0.98874,
                            0.90257,-0.50547,-0.06622,0.61541,-0.95208,0.95918,-0.63357,0.087663,0.48909,-0.89583,0.99033,-0.73892,
                            0.22886,0.36171,-0.82576,1,-0.82254,0.35506,0.23753,-0.74556,0.98901,-0.88173,0.46231,0.11951,-0.65623,
                            0.95683,-0.91511,0.54764,0.012432,-0.56356,0.90844,-0.92505,0.6102,-0.080016,-0.47272,0.84881,-0.91473,
                            0.65044,-0.15559,-0.38782,0.78248,-0.88766,0.66981,-0.21333,-0.31179,0.71332,-0.8474,0.67044,-0.25335,
                            -0.24645,0.64442,-0.79727,0.65484,-0.27661,-0.1926,0.57802,-0.74019,0.62569,-0.28473,-0.15017,0.51556,
                            -0.67861,0.58565,-0.27972,-0.1184,0.45775,-0.61448,0.53725,-0.26389,-0.095981,0.40471,-0.54929,0.48288,
                            -0.23968,-0.081247,0.35613,-0.48419,0.42471,-0.20955,-0.072329,0.31138,-0.42004,0.36476,-0.17592,
                            -0.067293,0.26974,-0.35753,0.30489,-0.14109,-0.064278,0.23048,-0.29733,0.24685,-0.10718,-0.061626,
                            0.19302,-0.24014,0.19228,-0.07606,-0.057997,0.15709,-0.18679,0.1427,-0.049279,-0.052487,0.12275,-0.13822,
                            0.099451,-0.027946,-0.044714,0.090496,-0.095509,0.063646,-0.012645,-0.034894,0.061213,-0.059753,0.03601,
                            -0.003327,-0.023868,0.036134,-0.031945,0.016746,0.00077575,-0.01307,0.016688,-0.012774,0.0053751,0.0011915,
                            -0.0044238,0.0042797,-0.0023872,0.00059199,0.00017943,-0.0001371,0};

void rawToMagnitude(float *raw, int rawSize, int startIdx, int endIdx, float *dest, int destSize) {
    // Three stages: 1. convolution & abs 2. subsampling
    // raw should be only 1ch and 1tr
    float* conv = (float *) malloc(sizeof(float) * rawSize);
    makeConvolveInDestSize(raw, rawSize, &SIGNAL_CORRELATE[0], 1200, conv, rawSize, true);
    // downsampling
    float* intermed = (float *) malloc(sizeof(float) * (endIdx - startIdx));
    memcpy(intermed, conv + startIdx, sizeof(float) * (endIdx - startIdx));
    debug("use the new signal");
    int i = 0;
    int step = (endIdx - startIdx) / destSize;
    for(int k = 0; k < destSize; ++k) {
        dest[k] = intermed[i];
        i += step;
    }
}

// this is equal to conv(...,'same') in matlab
// NOTE: this will only make the size of dest array convolve -> for speed optimization
void makeConvolveInDestSize(float *source, int sourceSize, float *filter, int filterSize, float *dest, int destSize, bool returnAbs){
    int validSize = destSize;

    int startPaddingSize = filterSize/2 -((filterSize+1)%2); // if filterSize is odd -> reduce one more startPadding for fitting matlab design
    int endPaddingSize = filterSize/2;
    int startPadding = 0;
    int endPadding = 0;
    int referIdx = 0; // the refer of original vector
    for(int i=0;i<validSize;i++){
        float sum = 0;
        int sourceIdx = referIdx;

        if(i<startPaddingSize){ // need start padding
            startPadding = startPaddingSize - i;
        } else {
            startPadding = 0;
        }

        if(i>=sourceSize-endPaddingSize){
            endPadding = i-sourceSize+endPaddingSize+1;
        } else {
            endPadding = 0;
        }

        //debug("i=%d, souceIdx=%d, sp=%d, ep=%d", i, sourceIdx, startPadding, endPadding);

        for(int filterIdx=filterSize-1-startPadding;filterIdx>=0+endPadding;filterIdx--){
            sum += source[sourceIdx]*filter[filterIdx];
            sourceIdx ++;
        }

        // flip sum if abs is required
        if(returnAbs && sum < 0) sum = -1*sum;
        dest[i] = sum;

        // only move the refer idx when no start padding is need
        if(startPadding == 0){
            referIdx++;
        }
    }
}
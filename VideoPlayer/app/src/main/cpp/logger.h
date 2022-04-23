//
// Created by bytedance on 2022/4/3.
//

#ifndef VIDEOPLAYER_LOGGER_H
#define VIDEOPLAYER_LOGGER_H

#include <android/log.h>
#define TAG  "VIDEO_PLAYER"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG,__VA_ARGS__)


#include <SLES/OpenSLES_Android.h>

static __inline__ void logSLresult(SLresult result, const char *tag) {

    if (result != SL_RESULT_SUCCESS) {
        switch (result) {
            case SL_RESULT_PRECONDITIONS_VIOLATED    : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_PRECONDITIONS_VIOLATED");
                break;
            }
            case SL_RESULT_PARAMETER_INVALID        : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_PARAMETER_INVALID");
                break;
            }
            case SL_RESULT_MEMORY_FAILURE : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_MEMORY_FAILURE");
                break;
            }
            case SL_RESULT_RESOURCE_ERROR : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_RESOURCE_ERROR");
                break;
            }
            case SL_RESULT_RESOURCE_LOST : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_RESOURCE_LOST");
                break;
            }
            case SL_RESULT_IO_ERROR : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_IO_ERROR");
                break;
            }
            case SL_RESULT_BUFFER_INSUFFICIENT : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_BUFFER_INSUFFICIENT");
                break;
            }
            case SL_RESULT_CONTENT_CORRUPTED : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_CONTENT_CORRUPTED");
                break;
            }
            case SL_RESULT_CONTENT_UNSUPPORTED : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_CONTENT_UNSUPPORTED");
                break;
            }
            case SL_RESULT_CONTENT_NOT_FOUND : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_CONTENT_NOT_FOUND");
                break;
            }
            case SL_RESULT_PERMISSION_DENIED : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_PERMISSION_DENIED");
                break;
            }
            case SL_RESULT_FEATURE_UNSUPPORTED : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_FEATURE_UNSUPPORTED");
                break;
            }
            case SL_RESULT_INTERNAL_ERROR : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_INTERNAL_ERROR");
                break;
            }
            case SL_RESULT_UNKNOWN_ERROR : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_UNKNOWN_ERROR");
                break;
            }
            case SL_RESULT_OPERATION_ABORTED : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_OPERATION_ABORTED");
                break;
            }
            case SL_RESULT_CONTROL_LOST : {
                LOGD("tag:%s,result:%s", tag, "SL_RESULT_CONTROL_LOST");
                break;
            }
        }
    }
} ;

#endif //VIDEOPLAYER_LOGGER_H

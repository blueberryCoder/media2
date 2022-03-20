//
// Created by bytedance on 2022/3/20.
//

#ifndef GLEXAMPLE_LOGGER_H
#define GLEXAMPLE_LOGGER_H
#include <android/log.h>
#define TAG  "EGL"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

#endif //GLEXAMPLE_LOGGER_H

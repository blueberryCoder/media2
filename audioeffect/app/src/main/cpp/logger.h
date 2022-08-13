//
// Created by bytedance on 2022/7/10.
//

#ifndef AUDIOEFFECT_LOGGER_H
#define AUDIOEFFECT_LOGGER_H

#include "android/log.h"

#define TAG  "audioeffect-jni"

#define logi(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)

#endif //AUDIOEFFECT_LOGGER_H

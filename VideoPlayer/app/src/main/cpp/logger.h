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


#endif //VIDEOPLAYER_LOGGER_H

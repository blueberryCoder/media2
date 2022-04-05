//
// Created by bytedance on 2022/4/3.
//

#ifndef VIDEOPLAYER_AUDIO_COMMON_H
#define VIDEOPLAYER_AUDIO_COMMON_H

#include  <cassert>
#include <SLES/OpenSLES_Android.h>
#include <SLES/OpenSLES.h>

#define BUF_COUNT 16
#define AUDIO_SAMPLE_CHANNELS 1
#define DEVICE_SHADOW_BUFFER_QUEUE_LEN 4
#define ENGINE_SERVICE_MSG_RETRIEVE_DUMP_BUFS 2
#define ENGINE_SERVICE_MSG_RECORDED_AUDIO_AVAILABLE 3

#define PLAY_KICKSTART_BUFFER_COUNT 3
#define RECORD_DEVICE_KICKSTART_BUF_COUNT 2


struct SampleFormat {
    uint32_t sampleRate_;
    uint32_t framesPerBuf_;
    uint16_t channels_;
    uint16_t pcmFormat_;
    uint32_t representation_;
};

extern void ConvertToSLSampleFormat(SLAndroidDataFormat_PCM_EX_ *pFormat, SampleFormat *format);

#define SLASSERT(x)                      \
    do {                                 \
       assert(SL_RESULT_SUCCESS ==(x)) ; \
       (void)(x);                        \
    }while(0)
#endif //VIDEOPLAYER_AUDIO_COMMON_H

typedef bool (*ENGINE_CALLBACK)(void *pCTX, uint32_t msg, void *pData);
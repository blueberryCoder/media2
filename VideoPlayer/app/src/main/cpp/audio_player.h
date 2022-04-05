//
// Created by bytedance on 2022/4/4.
//

#ifndef VIDEOPLAYER_AUDIO_PLAYER_H
#define VIDEOPLAYER_AUDIO_PLAYER_H

#include "audio_common.h"
#include "buf_manager.h"
#include <mutex>
#include <stdint.h>

class AudioPlayer {

    SLObjectItf outputMixObjectItf_;
    SLObjectItf playerObjectItf_;
    SLPlayItf playItf_;
    SLAndroidSimpleBufferQueueItf playBufferQueueItf_;

    SampleFormat sampleInfo_;
    AudioQueue *freeQueue_;
    AudioQueue *playQueue_;
    AudioQueue *devShadowQueue_;

    ENGINE_CALLBACK callback_;
    void *ctx_;
    sample_buf silentBuf_;
    std::mutex stopMutex_;

public:
    explicit AudioPlayer(SampleFormat *sampleFormat,SLEngineItf engine);
    ~AudioPlayer();
    void SetBufQueue(AudioQueue * playQ,AudioQueue * freeQ);
    SLresult Start();
    void Stop();
    void ProcessSLCallback(SLAndroidSimpleBufferQueueItf bq);
    uint32_t dbgGetDevBufCount(void);
    void RegisterCallback(ENGINE_CALLBACK cb,void *ctx);
};

#endif //VIDEOPLAYER_AUDIO_PLAYER_H

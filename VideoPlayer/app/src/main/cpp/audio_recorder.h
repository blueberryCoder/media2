//
// Created by bytedance on 2022/4/4.
//

#ifndef VIDEOPLAYER_AUDIO_RECORDER_H
#define VIDEOPLAYER_AUDIO_RECORDER_H

#include <stdint.h>
#include "audio_common.h"
#include "buf_manager.h"

class AudioRecorder {
    SLObjectItf recObjectItf_;
    SLRecordItf recItf_;
    SLAndroidSimpleBufferQueueItf recBufQueueItf_;
    SampleFormat sampleInfo_;
    AudioQueue *freeQueue_;
    AudioQueue *recQueue_;
    AudioQueue *devShadowQueue_;
    uint32_t audioBufCount;
    ENGINE_CALLBACK callback_;
    void *ctx_;
public :
    explicit AudioRecorder(SampleFormat *, SLEngineItf engineEngine);

    ~AudioRecorder();

    SLboolean Start();

    SLboolean Stop();

    void SetBufQueues(AudioQueue *freeQ, AudioQueue *recQ);

    void ProcessSLCallback(SLAndroidSimpleBufferQueueItf bq);

    void RegisterCallback(ENGINE_CALLBACK cb, void *ctx);

    int32_t dbgGetDevBufCount();

};

#endif //VIDEOPLAYER_AUDIO_RECORDER_H

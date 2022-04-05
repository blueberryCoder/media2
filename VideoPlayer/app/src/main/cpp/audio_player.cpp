//
// Created by bytedance on 2022/4/4.
//

#include "audio_player.h"
#include "logger.h"

void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *ctx) {
    (static_cast<AudioPlayer *>(ctx)->ProcessSLCallback(bq));
}

void AudioPlayer::ProcessSLCallback(SLAndroidSimpleBufferQueueItf bq) {
    std::lock_guard<std::mutex> lock(stopMutex_);
    LOGD("AudioPlayer::ProcessSLCallback bq");
    sample_buf *buf;
    if (!devShadowQueue_->front(&buf)) {
        if (callback_) {
            uint32_t count;
            callback_(ctx_, ENGINE_SERVICE_MSG_RETRIEVE_DUMP_BUFS, &count);
        }
        LOGD("AudioPlayer::ProcessSLCallback return no front");
        return;
    }
    devShadowQueue_->pop();
    if (buf != &silentBuf_) {
        buf->size_ = 0;
        // 将播放完的buf放入到freeQueue中
        freeQueue_->push(buf);
        if (!playQueue_->front(&buf)) {
            return;
        }
        // 跟踪播放数据
        devShadowQueue_->push(buf);
        (*bq)->Enqueue(bq, buf->buf_, buf->size_);
        playQueue_->pop();
        LOGD("AudioPlayer:ProcessSLCallback buf!=&silentBuf_ consume playQueue");
        return;
    }
    if (playQueue_->size() < PLAY_KICKSTART_BUFFER_COUNT) {
        // 刚开始的场景:
        // buf==&silentBuf_ &&  payQueue小于3的时候，提前push3个silentBuf进去
        (*bq)->Enqueue(bq, buf->buf_, buf->size_);
        devShadowQueue_->push(&silentBuf_);
        LOGD("AudioPlayer:ProcessSLCallback buf==&silentBuf_ just started . put silent buf.");
        return;
    }
    assert(PLAY_KICKSTART_BUFFER_COUNT <=
           (DEVICE_SHADOW_BUFFER_QUEUE_LEN - devShadowQueue_->size()));
    for (int32_t idx = 0; idx < PLAY_KICKSTART_BUFFER_COUNT; idx++) {
        // 刚开始的场景:
        // buf==&silentBuf_ &&  payQueue不小于3的时候，塞3个payQueue的buf进去。
        LOGD("AudioPlayer:ProcessSLCallback buf==&silentBuf_ just started . put pay queue buf.");
        playQueue_->front(&buf);
        playQueue_->pop();
        devShadowQueue_->push(buf);
        (*bq)->Enqueue(bq, buf->buf_, buf->size_);
    }
}

AudioPlayer::AudioPlayer(SampleFormat *sampleFormat, SLEngineItf slEngine) : freeQueue_(nullptr),
                                                                             playQueue_(nullptr),
                                                                             devShadowQueue_(
                                                                                     nullptr),
                                                                             callback_(nullptr) {
    SLresult result;
    assert(sampleFormat);
    sampleInfo_ = *sampleFormat;
    result = (*slEngine)->CreateOutputMix(slEngine, &outputMixObjectItf_, 0, NULL, NULL);
    SLASSERT(result);
    result = (*outputMixObjectItf_)->Realize(outputMixObjectItf_, SL_BOOLEAN_FALSE);
    SLASSERT(result);

    SLDataLocator_AndroidSimpleBufferQueue locBufQ = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, DEVICE_SHADOW_BUFFER_QUEUE_LEN};
    SLAndroidDataFormat_PCM_EX_ format_pcm;
    ConvertToSLSampleFormat(&format_pcm, &sampleInfo_);
    SLDataSource audioSrc = {&locBufQ, &format_pcm};

    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObjectItf_};
    SLDataSink audioSink = {&loc_outmix, NULL};
    SLInterfaceID ids[2] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
    SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    result = (*slEngine)->CreateAudioPlayer(slEngine, &playerObjectItf_, &audioSrc,
                                            &audioSink, sizeof(ids) / sizeof(ids[0]), ids, req);
    SLASSERT(result);
    // realize the player
    result = (*playerObjectItf_)->Realize(playerObjectItf_, SL_BOOLEAN_FALSE);
    SLASSERT(result);
    result = (*playerObjectItf_)->GetInterface(playerObjectItf_, SL_IID_PLAY,
                                               &playItf_);
    SLASSERT(result);
    result = (*playerObjectItf_)->GetInterface(playerObjectItf_,
                                               SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                               &playBufferQueueItf_);
    SLASSERT(result);
    result = (*playBufferQueueItf_)->RegisterCallback(playBufferQueueItf_, bqPlayerCallback, this);
    SLASSERT(result);
    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_STOPPED);
    SLASSERT(result);

    // create an empty queue to track deviceQueue.
    devShadowQueue_ = new AudioQueue(DEVICE_SHADOW_BUFFER_QUEUE_LEN);
    assert(devShadowQueue_);

    silentBuf_.cap_ = (format_pcm.containerSize >> 3) * format_pcm.numChannels
                      * sampleInfo_.framesPerBuf_;
    silentBuf_.buf_ = new uint8_t[silentBuf_.cap_];
    memset(silentBuf_.buf_, 0, silentBuf_.cap_);
    silentBuf_.size_ = silentBuf_.cap_;
}

AudioPlayer::~AudioPlayer() {
    std::lock_guard<std::mutex> lock(stopMutex_);
    if (playerObjectItf_ != nullptr) {
        (*playerObjectItf_)->Destroy(playerObjectItf_);
    }
    sample_buf *buf = nullptr;
    while (devShadowQueue_->front(&buf)) {
        buf->size_ = 0;
        devShadowQueue_->pop();
        if (buf != &silentBuf_) {
            freeQueue_->push(buf);
        }
    }
    while (playQueue_->front(&buf)) {
        buf->size_ = 0;
        playQueue_->pop();
        freeQueue_->push(buf);
    }
    if (outputMixObjectItf_) {
        (*outputMixObjectItf_)->Destroy(outputMixObjectItf_);
    }
    delete[] silentBuf_.buf_;
}

void AudioPlayer::SetBufQueue(AudioQueue *playQ, AudioQueue *freeQ) {
    playQueue_ = playQ;
    freeQueue_ = freeQ;
}

SLresult AudioPlayer::Start() {
    SLuint32 state;
    SLresult result = (*playItf_)->GetPlayState(playItf_, &state);
    if (result != SL_RESULT_SUCCESS) {
        return SL_BOOLEAN_FALSE;
    }
    if (state == SL_PLAYSTATE_PLAYING) {
        return SL_BOOLEAN_TRUE;
    }
    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_STOPPED);
    SLASSERT(result);

    // 插入buffer
    result = (*playBufferQueueItf_)->Enqueue(playBufferQueueItf_, silentBuf_.buf_,
                                             silentBuf_.size_);
    SLASSERT(result);
    devShadowQueue_->push(&silentBuf_);
    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_PLAYING);
    SLASSERT(result);
    return SL_BOOLEAN_TRUE;
}

void AudioPlayer::Stop() {
    SLuint32 state;
    SLresult result = (*playItf_)->GetPlayState(playItf_, &state);
    SLASSERT(result);
    if (state == SL_PLAYSTATE_STOPPED) return;
    std::lock_guard<std::mutex> lock(stopMutex_);
    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_STOPPED);
    SLASSERT(result);
    (*playBufferQueueItf_)->Clear(playBufferQueueItf_);
}

void AudioPlayer::RegisterCallback(ENGINE_CALLBACK cb, void *ctx) {
    callback_ = cb;
    ctx_ = ctx;
}

uint32_t AudioPlayer::dbgGetDevBufCount() {
    return (devShadowQueue_->size());
}
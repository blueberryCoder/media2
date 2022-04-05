//
// Created by bytedance on 2022/4/3.
//

#include <jni.h>
#include <SLES/OpenSLES_Android.h>
#include <cstring>
#include "jni_interfaces.h"
#include "logger.h"
#include "audio_common.h"
#include "buf_manager.h"
#include "audio_effect.h"
#include "audio_player.h"
#include "audio_recorder.h"


struct EchoAudioEngine {
    SLmilliHertz fastPathSampleRate_;
    uint32_t fastPathFramesPerBuf_;
    uint16_t sampleChannels_;
    uint16_t bitsPerSample_;


    SLObjectItf slEngineObj_;
    SLEngineItf slEngineItf_;

    AudioRecorder *recorder_;
    AudioPlayer *player_;
    AudioQueue *freeBufQueue_; // Owner of the queue
    AudioQueue *recBufQueue_;  // Owner of the queue , is playQueue for audioPlayer ,is RecQueue for Recoder

    sample_buf *bufs_;
    uint32_t bufCount_;

    uint32_t frameCount_;
    int64_t echoDelay_;
    float echoDecay_;
    AudioDelay *delayEffect_;

};

static EchoAudioEngine engine;
extern "C" {

uint32_t dbgEngineGetBufCount() {
    uint32_t count = engine.player_->dbgGetDevBufCount();
    count += engine.recorder_->dbgGetDevBufCount();
    count += engine.freeBufQueue_->size();
    count += engine.recBufQueue_->size();
    LOGE("Buf Distributions: PlayerDev=%d,RecDev=%d,FreeQ=%d,RecQ=%d",
         engine.player_->dbgGetDevBufCount(),
         engine.recorder_->dbgGetDevBufCount(),
         engine.freeBufQueue_->size(),
         engine.recBufQueue_->size());
    if (count != engine.bufCount_) {
        LOGE("=======Lost Bufs among the queue(supposed = %d, found = %d)", BUF_COUNT, count);
    }
    return count;
}

bool EngineService(void *ctx, uint32_t msg, void *data) {
    assert(ctx == &engine);
    switch (msg) {
        case ENGINE_SERVICE_MSG_RETRIEVE_DUMP_BUFS: {
            *(static_cast<uint32_t *>(data)) = dbgEngineGetBufCount();
            break;
        }
        case ENGINE_SERVICE_MSG_RECORDED_AUDIO_AVAILABLE: {
            sample_buf *buf = static_cast<sample_buf *>(data);
            assert(engine.fastPathFramesPerBuf_ ==
                   buf->size_ / engine.sampleChannels_ / engine.sampleChannels_ /
                   (engine.bitsPerSample_ / 8));
            engine.delayEffect_->process(reinterpret_cast<int16_t *>(buf->buf_),
                                         engine.fastPathFramesPerBuf_);
            break;
        }
        default:
            assert(false);
            return false;
    }
    return true;
}

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_createSLEngine(JNIEnv *env, jclass clazz,
        // 48000
                                                       jint sampleRate,
        // 96
                                                       jint framesPerBuf,
        // 100ms
                                                       jlong delayInMs,
        // 0.1
                                                       jfloat decay) {
    LOGD("createSLEngine.");
    SLresult result;
    memset(&engine, 0, sizeof(engine));
    // 1Hz = 1000 millihertz
    engine.fastPathSampleRate_ = static_cast<SLmilliHertz >(sampleRate) * 1000;
    // 96
    engine.fastPathFramesPerBuf_ = static_cast<uint32_t >(framesPerBuf);
    engine.sampleChannels_ = AUDIO_SAMPLE_CHANNELS;
    engine.bitsPerSample_ = SL_PCMSAMPLEFORMAT_FIXED_16;

    result = slCreateEngine(&engine.slEngineObj_, 0,
                            NULL, 0,
                            NULL, NULL);
    SLASSERT(result);
    result = (*engine.slEngineObj_)->Realize(engine.slEngineObj_, SL_BOOLEAN_FALSE);
    SLASSERT(result);
    result = (*engine.slEngineObj_)->GetInterface(engine.slEngineObj_, SL_IID_ENGINE,
                                                  &engine.slEngineItf_);
    SLASSERT(result);

    // 根据推荐推荐的没个buf多少帧(framesPerBuf)*channel数*每个样本多少bits来计算buf的大小。
    uint32_t bufSize =
            engine.fastPathFramesPerBuf_ * engine.sampleChannels_ * engine.bitsPerSample_;
    bufSize = (bufSize + 7) >> 3;
    engine.bufCount_ = BUF_COUNT;
    // 分配16个buf
    engine.bufs_ = allocateSampleBufs(engine.bufCount_, bufSize);
    assert(engine.bufs_);
    engine.freeBufQueue_ = new AudioQueue(engine.bufCount_);
    engine.recBufQueue_ = new AudioQueue(engine.bufCount_);
    assert(engine.freeBufQueue_ && engine.recBufQueue_);

    for (int i = 0; i < engine.bufCount_; ++i) {
        engine.freeBufQueue_->push(&engine.bufs_[i]);
    }
    engine.echoDelay_ = delayInMs;
    engine.echoDecay_ = decay;
    engine.delayEffect_ = new AudioDelay(engine.fastPathSampleRate_,
                                         engine.sampleChannels_,
                                         engine.bitsPerSample_,
                                         engine.echoDelay_,
                                         engine.echoDecay_);
    assert(engine.delayEffect_);
    LOGD("engine:fastPathSampleRate_:%d,sampleChannels:%d,bitsPerSample:%d,echoDelay:%lld,echoDecay:%f",
         engine.fastPathSampleRate_,
         engine.sampleChannels_,
         engine.bitsPerSample_,
         engine.echoDelay_,
         engine.echoDecay_
    );
}

JNIEXPORT jboolean JNICALL
Java_com_blueberry_videoplayer_SLJniLib_createSLBufferQueueAudioPlayer(JNIEnv *env, jclass clazz) {
    LOGD("createSLBufferQueueAudioPlayer");
    SampleFormat sampleFormat;
    memset(&sampleFormat, 0, sizeof(sampleFormat));
    sampleFormat.pcmFormat_ = (uint16_t) engine.bitsPerSample_;
    sampleFormat.framesPerBuf_ = engine.fastPathFramesPerBuf_;
    sampleFormat.channels_ = engine.sampleChannels_;
    sampleFormat.sampleRate_ = engine.fastPathSampleRate_;

    engine.player_ = new AudioPlayer(&sampleFormat, engine.slEngineItf_);
    assert(engine.player_);
    if (engine.player_ == nullptr) return JNI_FALSE;
    // the recBufQueue will be playQueue
    engine.player_->SetBufQueue(engine.recBufQueue_, engine.freeBufQueue_);
    engine.player_->RegisterCallback(EngineService, (void *) &engine);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_blueberry_videoplayer_SLJniLib_createAudioRecorder(JNIEnv *env, jclass clazz) {
    LOGD("createAudioRecorder");
    SampleFormat sampleFormat;
    memset(&sampleFormat, 0, sizeof(sampleFormat));
    sampleFormat.pcmFormat_ = static_cast<uint16_t >(engine.bitsPerSample_);
    sampleFormat.channels_ = engine.sampleChannels_;
    sampleFormat.sampleRate_ = engine.fastPathSampleRate_;
    sampleFormat.framesPerBuf_ = engine.fastPathFramesPerBuf_;
    engine.recorder_ = new AudioRecorder(&sampleFormat, engine.slEngineItf_);
    if (!engine.recorder_) {
        return JNI_FALSE;
    }
    engine.recorder_->SetBufQueues(engine.freeBufQueue_, engine.recBufQueue_);
    engine.recorder_->RegisterCallback(EngineService, (void *) &engine);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_deleteAudioRecorder(JNIEnv *env, jclass clazz) {
    if (engine.recorder_) delete engine.recorder_;
    engine.recorder_ = nullptr;
}

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_startPlay(JNIEnv *env, jclass clazz) {
    engine.frameCount_ = 0;
    if (SL_BOOLEAN_FALSE == engine.player_->Start()) {
        LOGE("====%s failed", __FUNCTION__);
        return;
    }
    engine.recorder_->Start();
}

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_stopPlay(JNIEnv *env, jclass clazz) {
    engine.recorder_->Stop();
    engine.player_->Stop();
    delete engine.recorder_;
    delete engine.player_;
    engine.player_ = nullptr;
    engine.recorder_ = nullptr;
}

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_deleteSLEngine(JNIEnv *env, jclass clazz) {
    delete engine.recBufQueue_;
    delete engine.freeBufQueue_;
    releaseSampleBufs(engine.bufs_, engine.bufCount_);
    if (engine.slEngineObj_ != nullptr) {
        (*engine.slEngineObj_)->Destroy(engine.slEngineObj_);
        engine.slEngineObj_ = nullptr;
        engine.slEngineItf_ = nullptr;
    }
    if (engine.delayEffect_) {
        delete engine.delayEffect_;
        engine.delayEffect_ = nullptr;
    }
}
JNIEXPORT jboolean JNICALL
Java_com_blueberry_videoplayer_SLJniLib_configureEcho(JNIEnv *env, jclass clazz, jint delayInMs,
                                                      jfloat decay) {
    engine.echoDelay_ = delayInMs;
    engine.echoDecay_ = decay;
    engine.delayEffect_->setDelayTime(delayInMs);
    engine.delayEffect_->setDecayWeight(decay);
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_blueberry_videoplayer_SLJniLib_deleteSLBufferQueueAudioPlayer(JNIEnv *env, jclass clazz) {
    if (engine.player_) {
        delete engine.player_;
        engine.player_ = nullptr;
    }
}

}

//
// Created by bytedance on 2022/4/3.
//

#include "audio_effect.h"

static const int32_t kFloatToIntMapFactor = 128;
static const uint32_t kMsPerSec = 1000;

AudioDelay::AudioDelay(int32_t sampleRate, int32_t channelCount, SLuint32 format,
                       size_t delayTimeInMs, float weight) : AudioFormat(sampleRate, channelCount,
                                                                         format),
                                                             delayTime_(delayTimeInMs),
                                                             decayWeight_(weight) {

    feedbackFactor_ = static_cast<int32_t >(decayWeight_ * kFloatToIntMapFactor);
    liveAudioFactor_ = kFloatToIntMapFactor - feedbackFactor_;

    allocateBuffer();
}

AudioDelay::~AudioDelay() {
    if (buffer_) delete static_cast<uint8_t *>(buffer_);
}

bool AudioDelay::setDelayTime(size_t delayTimeInMiliSec) {
    if (delayTime_ == delayTimeInMiliSec) return true;
    std::lock_guard<std::mutex> lock(lock_);
    if (buffer_) {
        delete static_cast<uint8_t *>(buffer_);
        buffer_ = nullptr;
    }
    delayTime_ = delayTimeInMiliSec;
    allocateBuffer();
    return buffer_ != nullptr;
}

size_t AudioDelay::getDelayTime() const { return delayTime_; }

void AudioDelay::setDecayWeight(float weight) {
    if (weight > 0.0f && weight < 1.0f) {
        float feedback = (weight * kFloatToIntMapFactor + 0.5);
        feedbackFactor_ = static_cast<int32_t >(feedback);
        liveAudioFactor_ = kFloatToIntMapFactor - feedbackFactor_;
    }
}

float AudioDelay::getDecayWeight() const {
    return decayWeight_;
}

void AudioDelay::allocateBuffer() {

    float floatDelayTime = (float) delayTime_ / kMsPerSec;
    // num of frame in per second.
    float fNumFrames = floatDelayTime * (sampleRate_ / kMsPerSec);
    size_t sampleCount = static_cast<uint32_t >(fNumFrames + 0.5f) * channelCount_;
    uint32_t bytePerSample = format_ / 8;
    assert(bytePerSample <= 4 && bytePerSample);
    uint32_t bytePerFrame = channelCount_ * bytePerSample;
    bufCapacity_ = sampleCount * bytePerSample;
    bufCapacity_ = ((bufCapacity_ + bytePerFrame - 1) / bytePerFrame) * bytePerFrame;
    buffer_ = new uint8_t[bufCapacity_];
    assert(buffer_);
    memset(buffer_, 0, bufCapacity_);
    curPos_ = 0;
    bufSize_ = bufCapacity_ / bytePerSample;
}

void AudioDelay::process(int16_t *liveAudio, int32_t numFrames) {
    if (feedbackFactor_ == 0 || bufSize_ < numFrames) {
        return;
    }
    if (!lock_.try_lock()) {
        return;
    }
    if (numFrames + curPos_ > bufSize_) {
        curPos_ = 0;
    }
    int32_t sampleCount = channelCount_ * numFrames;
    int16_t *samples = &static_cast<int16_t *>(buffer_)[curPos_ * channelCount_];
    for (size_t idx = 0; idx < sampleCount; idx++) {
        int32_t curSample = (samples[idx] * feedbackFactor_ + liveAudio[idx] * liveAudioFactor_) /
                            kFloatToIntMapFactor;
        if (curSample > SHRT_MAX) {
            curSample = SHRT_MAX;
        } else if (curSample < SHRT_MIN) {
            curSample = SHRT_MIN;
        }
        liveAudio[idx] = samples[idx];
        samples[idx] = static_cast<int16_t >(curSample);
    }
    curPos_ += numFrames;
    lock_.unlock();
}
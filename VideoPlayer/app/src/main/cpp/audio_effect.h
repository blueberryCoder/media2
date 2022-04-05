//
// Created by bytedance on 2022/4/3.
//

#ifndef VIDEOPLAYER_AUDIO_EFFECT_H
#define VIDEOPLAYER_AUDIO_EFFECT_H

#include <stdint.h>
#include <mutex>
#include <SLES/OpenSLES_Android.h>

class AudioFormat {
protected:
    // milliHertz
    int32_t sampleRate_ = SL_SAMPLINGRATE_48;
    int32_t channelCount_ = 2;
    SLuint32 format_ = SL_PCMSAMPLEFORMAT_FIXED_16;

    AudioFormat(int32_t sampleRate, int32_t channelCount, SLuint32 format) :
            sampleRate_(sampleRate), channelCount_(channelCount), format_(format) {}

    virtual ~AudioFormat() {}
};

class AudioDelay : public AudioFormat {
public :
    ~AudioDelay();

    explicit AudioDelay( int32_t sampleRate, int32_t channelCount, SLuint32 format,
                        size_t delayTimeInMs, float weight);

    bool setDelayTime(size_t delayTimeInMiliSec);
    size_t getDelayTime(void) const;
    void setDecayWeight(float  weight);
    float getDecayWeight(void) const;
    void process(int16_t * lineAudio,int32_t numFrames);

private:
    size_t delayTime_ = 0;
    float decayWeight_ = 0.5;
    void * buffer_ = nullptr;
    size_t bufCapacity_ = 0;
    size_t bufSize_ = 0;
    size_t curPos_ = 0;
    std::mutex lock_;
    int32_t feedbackFactor_;
    int32_t liveAudioFactor_;
    void allocateBuffer(void);
};

#endif //VIDEOPLAYER_AUDIO_EFFECT_H

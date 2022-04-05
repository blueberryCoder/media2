//
// Created by bytedance on 2022/4/3.
//

#include "audio_common.h"
#include <string.h>

void ConvertToSLSampleFormat(SLAndroidDataFormat_PCM_EX_ *pFormat, SampleFormat *pSampleInfo_) {
    assert(pFormat);
    memset(pFormat, 0, sizeof(pFormat));
    pFormat->formatType = SL_DATAFORMAT_PCM;
    if (pSampleInfo_->channels_ <= 1) {
        pFormat->numChannels = 1;
        pFormat->channelMask = SL_SPEAKER_FRONT_LEFT;
    } else {
        pFormat->numChannels = 2;
        pFormat->channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    }
    pFormat->sampleRate = pSampleInfo_->sampleRate_;
    pFormat->endianness = SL_BYTEORDER_LITTLEENDIAN;
    pFormat->bitsPerSample = pSampleInfo_->pcmFormat_;
    pFormat->containerSize = pSampleInfo_->pcmFormat_;

    pFormat->representation = pSampleInfo_->representation_;
    switch (pFormat->representation) {
        case SL_ANDROID_PCM_REPRESENTATION_UNSIGNED_INT :
            pFormat->bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_8;
            pFormat->containerSize = SL_PCMSAMPLEFORMAT_FIXED_8;
            pFormat->formatType = SL_ANDROID_DATAFORMAT_PCM_EX;
            break;
        case SL_ANDROID_PCM_REPRESENTATION_SIGNED_INT:
            pFormat->bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
            pFormat->containerSize = SL_PCMSAMPLEFORMAT_FIXED_16;
            pFormat->formatType = SL_ANDROID_DATAFORMAT_PCM_EX;
            break;
        case SL_ANDROID_PCM_REPRESENTATION_FLOAT:
            pFormat->bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_32;
            pFormat->containerSize = SL_PCMSAMPLEFORMAT_FIXED_32;
            pFormat->formatType = SL_ANDROID_DATAFORMAT_PCM_EX;
            break;
        case 0:
            break;
        default:
            assert(0);
    }


}

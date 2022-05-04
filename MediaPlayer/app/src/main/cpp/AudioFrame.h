//
// Created by bytedance on 2022/5/2.
//

#ifndef MEDIAPLAYER_AUDIOFRAME_H
#define MEDIAPLAYER_AUDIOFRAME_H

#include <stdint.h>
#include <memory.h>
#include <iostream>

class AudioFrame {
public:
    int64_t pts_ = 0;
    size_t size_ = 0;
    uint8_t *buf_;

    uint8_t *getBuf() {
        return buf_;
    }

    AudioFrame(int64_t _pts, size_t _size, uint8_t *_buf) : pts_(_pts), size_(_size) {
        if (size_) {
            buf_ = new uint8_t[size_];
            memcpy(buf_, _buf, size_);
        }
    }

    ~AudioFrame() {
    }


    constexpr bool operator>(const AudioFrame &rhs) {
        return this->pts_ > rhs.pts_;
    }

};


#endif //MEDIAPLAYER_AUDIOFRAME_H

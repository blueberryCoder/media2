//
// Created by bytedance on 2022/5/2.
//

#ifndef MEDIAPLAYER_VIDEOFRAME_H
#define MEDIAPLAYER_VIDEOFRAME_H

#include <stdint.h>
#include <memory.h>
#include "logger.h"

class VideoFrame {
public:
    int64_t pts_ = 0;
    size_t size_ = 0;
    uint8_t *buf_ = nullptr;

    int32_t  width_ = 0;
    int32_t  height_ = 0;
    int32_t  color_format_ =0;


    VideoFrame(int64_t _pts, size_t _size, uint8_t *_buf,int32_t _width,int32_t _height,int32_t _color_format) : pts_(_pts),
    size_(_size),width_(_width),height_(_height) ,color_format_(_color_format){
        if (size_) {
            buf_ = new uint8_t[size_]{0};
//            LOGI("video frame : pts: %ld,size_ :%d",pts_,size_);
            memcpy(buf_, _buf, size_);
        }
    }

     bool operator>(const VideoFrame &rhs) const {
        return this->pts_ > rhs.pts_;
    }
};


#endif //MEDIAPLAYER_VIDEOFRAME_H

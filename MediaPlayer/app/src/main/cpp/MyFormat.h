//
// Created by bytedance on 2022/5/1.
//

#ifndef MEDIAPLAYER_MYFORMAT_H
#define MEDIAPLAYER_MYFORMAT_H

#include <stdint.h>
#include <string>

// https://juejin.cn/post/6844904067278307335
class MyFormat {

public:
//    const char mime[200]{};
    std::string mime_;
    int32_t sample_rate_;
    int32_t channel_count_;
    int32_t channel_mask_;
    int32_t max_bitrate_;

    // us
    int64_t duration_;

    int32_t  width_;
    int32_t  height_;
    int32_t  color_format_ ;

    // csd-0
    uint8_t * sps_;
    size_t sps_size_;
    // csd-1
    uint8_t  *pps_;
    size_t  pps_size_ ;


    static constexpr int COLOR_FormatYUV420Planar  = 19;

};


#endif //MEDIAPLAYER_MYFORMAT_H

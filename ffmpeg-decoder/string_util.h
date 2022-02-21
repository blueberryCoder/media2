//
// Created by bytedance on 2022/1/27.
//

#ifndef FFMPEG_DECODER_STRING_UTIL_H
#define FFMPEG_DECODER_STRING_UTIL_H

#include <iostream>
#include <vector>

static inline const std::string vformat(const char *const fmt, ...) {
    // Initialize use of the variable argument array.
    va_list vaArgs;
    va_start(vaArgs, fmt);
    va_list vaArgsCopy;
    __va_copy(vaArgsCopy, vaArgs);
    const int iLen = std::vsnprintf(nullptr, 0, fmt, vaArgsCopy);
    va_end(vaArgsCopy);
    std::vector<char> zc(iLen + 1);
    std::vsnprintf(zc.data(), zc.size(), fmt, vaArgs);
    va_end(vaArgs);
    return std::string(zc.data(), iLen);
}
#endif //FFMPEG_DECODER_STRING_UTIL_H

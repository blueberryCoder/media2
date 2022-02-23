# 使用ffmpeg解析mp4文件得到音频和视频数据
## 环境
``` sh 
➜  ~ ffmpeg -version
ffmpeg version N-105035-g617452ce2c Copyright (c) 2000-2021 the FFmpeg developers
built with Apple clang version 12.0.5 (clang-1205.0.22.11)
configuration: --enable-gpl --enable-filter=aresample --enable-bsf=aac_adtstoasc --enable-small --enable-dwt --enable-lsp --enable-mdct --enable-rdft --enable-fft --enable-static --enable-version3 --enable-nonfree --enable-encoder=libfdk_aac --enable-encoder=libx264 --enable-decoder=mp3 --enable-libx264 --enable-libfdk_aac --enable-libmp3lame --extra-cflags=-I/usr/local/include --extra-ldflags=-L/usr/local/lib
libavutil      57. 13.100 / 57. 13.100
libavcodec     59. 15.101 / 59. 15.101
libavformat    59. 10.100 / 59. 10.100
libavdevice    59.  1.100 / 59.  1.100
libavfilter     8. 21.100 /  8. 21.100
libswscale      6.  1.102 /  6.  1.102
libswresample   4.  0.100 /  4.  0.100
libpostproc    56.  0.100 / 56.  0.100
```
## 流程
1. 创建AVFormatContext
   - 使用avformat_alloc_context分配AVFormatContext。
   - 使用avformat_open_input打开输入的mp4文件
   - 使用avformat_find_stream_info获取mp4文件中的流信息。
2. 在AVFormatContext中获取视频流信息，流信息中包含编码器信息
    - 使用av_find_best_stream寻找视频流在AVFormatCondex中的index
    - 根据index获取到Stream。如:
    `AVStream *stream = format_context_->streams[*stream_index];`
3. 根据获取到的流信息找到合适的编码器并创建编码器上下文
    - 使用avcodec_find_decoder找到编码器。如：
    `const AVCodec *codec = avcodec_find_decoder(stream->codecpar->codec_id);`
    - 使用avcodec_alloc_context3创建编码器上下文：
    `*codec_context = avcodec_alloc_context3(codec);`
    - 使用avcodec_parameters_to_context将流中包含的一些信息如视频format、width、height等复制到AVCodecContext。如：
    `avcodec_parameters_to_context(*codec_context, stream->codecpar)`
4. 寻找音频流信息、创建音频编码器上下文，方法同上视频获取的方式。
5. 根据输出路径打开视频yuv文件输出流，音频pcm输出流。
6. 分配AVPacket、AVFrame、图像buffer
    - 使用av_packet_alloc分配AVPacket
    - 使用av_frame_alloc分配AVFrame
    - 使用av_image_alloc分配图片buffer
7. 使用av_read_frame开始读文件中的每一个packet
8. 根据读到的packet判断是否为音频/视频采用不同的解码器上下文进行解码
    - 使用avcodec_send_packet发送AVPacket给解码器。
    - 使用avcodec_receive_frame接受解码的结果获得AVFrame。
10. 将解码得到的AVFrame分别写入输出文件流
    - 输出音频
    ``` cpp
      int unpadded_line_size = frame->nb_samples * av_get_bytes_per_sample(AVSampleFormat(frame->format));
      std::cout << vformat("Write audio frame %d,size=%d", this->audio_frame_counter++, unpadded_line_size) << std::endl;
      output_audio_stream_.write(reinterpret_cast<const char *>(frame->extended_data[0]), unpadded_line_size);
    ```
    - 输出视频
    将frame中的图像数据复制到上述分配的图像buffer中，然后写入输出流
    ``` cpp
       av_image_copy(image_data_,
                  image_data_line_size_,
                  const_cast<const uint8_t ** > ( reinterpret_cast< uint8_t **>(frame->data)),
                  frame->linesize,
                  this->video_codec_context_->pix_fmt,
                  frame->width,
                  frame->height);
        output_video_stream_.write(reinterpret_cast<const char *>(this->image_data_[0]), this->image_dst_buffer_size);
    ```
## 代码实现
### 定义Demuxing类来实现分离的功能
```cpp
#ifndef FFMPEG_DECODER_DEMUXING_H
#define FFMPEG_DECODER_DEMUXING_H

extern "C" {
#include <libavformat/avformat.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <libavutil/pixdesc.h>
#include <libavutil/error.h>
#include <libavutil/imgutils.h>
#include <libavcodec/avcodec.h>
#include <libavutil/timestamp.h>
};

#include <fstream>
#include <iostream>

class Demuxing {

public:
    Demuxing();

    virtual ~Demuxing();

    /**
     * 初始化设置参数
     * @param inputFile  输入文件路径
     * @param outputVideo 输出视频文件路径
     * @param outputAudio 输入音频文件路径
     */
    void Initialize(std::string input_file, std::string output_video, std::string output_audio);

    void Start();

private:
    std::string input_file_;
    std::string output_video_;
    std::string output_audio_;

    std::fstream output_audio_stream_;
    std::fstream output_video_stream_;

    AVFormatContext *format_context_ = nullptr;
    AVCodecContext *audio_codec_context_ = nullptr;
    AVCodecContext *video_codec_context_ = nullptr;

    int audio_stream_index_ = -1;
    int video_stream_index_ = -1;

    int audio_frame_counter = 0;
    int video_frame_counter = 1;

    uint8_t *image_data_[4] = {nullptr};
    int image_data_line_size_[4] = {0};
    int image_dst_buffer_size = 0;

    AVPacket *av_packet_ = nullptr;
    AVFrame *av_frame_ = nullptr;

    void OpenCodecContext(AVMediaType type, AVCodecContext **codec_context, int *stream_index) const;

    int DecodePacket(AVCodecContext *codec_context);

    int OutputAudioFrame(AVFrame *frame);

    int OutputVideoFrame(AVFrame *frame);

    void AllocImage();
};
#endif //FFMPEG_DECODER_DEMUXING_H
```
### 实现
```cpp
#include "Demuxing.h"

#include "string_util.h"
#include <ios>

Demuxing::Demuxing() = default;

Demuxing::~Demuxing() {

    av_freep(this->image_data_);

    av_free(this->av_packet_);
    av_free(this->av_frame_);

    avcodec_free_context(&this->video_codec_context_);
    avcodec_free_context(&this->audio_codec_context_);
    avformat_close_input(&format_context_);
    avformat_free_context(format_context_);

    this->output_video_stream_.close();
    this->output_audio_stream_.close();
};

void Demuxing::Initialize(std::string input_file, std::string output_video, std::string output_audio) {
    this->input_file_ = std::move(input_file);
    this->output_audio_ = std::move(output_audio);
    this->output_video_ = std::move(output_video);

    format_context_ = avformat_alloc_context();
    avformat_open_input(&format_context_, this->input_file_.c_str(), nullptr, nullptr);
    avformat_find_stream_info(format_context_, nullptr);

    OpenCodecContext(AVMEDIA_TYPE_VIDEO,
                     &this->video_codec_context_, &this->video_stream_index_);
    OpenCodecContext(AVMEDIA_TYPE_AUDIO,
                     &this->audio_codec_context_, &this->audio_stream_index_);

    this->output_audio_stream_.open(this->output_audio_.c_str(), std::ios::binary | std::ios::out);
    this->output_video_stream_.open(this->output_video_.c_str(), std::ios::binary | std::ios::out);

    if (this->output_video_stream_.bad()) {
        throw std::runtime_error("Open output video file failed.");
    }
    if (this->output_audio_stream_.bad()) {
        throw std::runtime_error("Open output audio file failed.");
    }
}

void Demuxing::Start() {
    av_dump_format(this->format_context_, 0, this->input_file_.c_str(), 0);

    this->av_packet_ = av_packet_alloc();
    this->av_frame_ = av_frame_alloc();
    AllocImage();
    if (this->av_packet_ == nullptr) {
        throw std::runtime_error("alloc packet failure.");
    }
    if (this->av_frame_ == nullptr) {
        throw std::runtime_error("alloc avframe failure.");
    }

    while (av_read_frame(this->format_context_, this->av_packet_) >= 0) {
        int ret = 0;
        if (this->av_packet_->stream_index == this->audio_stream_index_) {
            // decode audio data
            DecodePacket(this->audio_codec_context_);
        } else if (this->av_packet_->stream_index == this->video_stream_index_) {
            // decode video data
            DecodePacket(this->video_codec_context_);
        }
        av_packet_unref(this->av_packet_);
        if (ret < 0) {
            break;
        }
    }
}

int Demuxing::DecodePacket(AVCodecContext *codec_context) {
    int ret = 0;

    ret = avcodec_send_packet(codec_context, this->av_packet_);

    if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
        return 0;
    }
    if (ret == AVERROR(EINVAL)) {
        throw std::runtime_error(vformat("avcodec_send_packet failure. error:%s", av_err2str(ret)));
    }
    if (ret == AVERROR_INPUT_CHANGED) {
        throw std::runtime_error(vformat("avcodec_send_packet failure. error:%s", av_err2str(ret)));
    }
    if (ret < 0) {
        throw std::runtime_error(vformat("avcodec_send_packet failure. error:%s", av_err2str(ret)));
    }

    while (ret >= 0) {
        ret = avcodec_receive_frame(codec_context, this->av_frame_);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            return 0;
        }
        if (ret == AVERROR(EINVAL)) {
            throw std::runtime_error(vformat("avcodec_receive_failure. error:%s", av_err2str(ret)));
        }
        if (ret == AVERROR_INPUT_CHANGED) {
            throw std::runtime_error(vformat("avcodec_receive_failure failure. error:%s", av_err2str(ret)));
        }
        if (ret < 0) {
            return ret;
        }
        if (codec_context->codec_type == AVMEDIA_TYPE_AUDIO) {
            ret = OutputAudioFrame(this->av_frame_);
        } else if (codec_context->codec_type == AVMEDIA_TYPE_VIDEO) {
            ret = OutputVideoFrame(this->av_frame_);
        }
        av_frame_unref(this->av_frame_);
        // if dump frame failed , return ret;
        if (ret < 0) {
            return ret;
        }
    }

    return 0;
}

int Demuxing::OutputAudioFrame(AVFrame *frame) {
    int unpadded_line_size = frame->nb_samples * av_get_bytes_per_sample(AVSampleFormat(frame->format));
    std::cout << vformat("Write audio frame %d,size=%d", this->audio_frame_counter++, unpadded_line_size) << std::endl;
    output_audio_stream_.write(reinterpret_cast<const char *>(frame->extended_data[0]), unpadded_line_size);
}


int Demuxing::OutputVideoFrame(AVFrame *frame) {
    if (frame->width != this->video_codec_context_->width
        || frame->height != this->video_codec_context_->height
        || frame->format != this->video_codec_context_->pix_fmt) {
        throw std::runtime_error("The video frame width,height and fmt must same .");
    }
    av_image_copy(image_data_,
                  image_data_line_size_,
                  const_cast<const uint8_t ** > ( reinterpret_cast< uint8_t **>(frame->data)),
                  frame->linesize,
                  this->video_codec_context_->pix_fmt,
                  frame->width,
                  frame->height
    );
    std::cout << vformat("Write video frame %d,size=%d,width=%d,height=%d,fmt=%s",
                         this->video_frame_counter++,
                         this->image_dst_buffer_size,
                         frame->width,
                         frame->height,
                         av_get_pix_fmt_name(AVPixelFormat(frame->format)))
              << std::endl;
    output_video_stream_.write(reinterpret_cast<const char *>(this->image_data_[0]), this->image_dst_buffer_size);
}

/**
 *
 * @param type 音频/视频
 */
void Demuxing::OpenCodecContext(AVMediaType type, AVCodecContext **codec_context, int *stream_index) const {
    // find video
    *stream_index = av_find_best_stream(format_context_,
                                        type, -1,
                                        -1, nullptr, 0);

    if (*stream_index < 0) {
        // stream not found
        throw std::runtime_error(vformat("Find stream %s error :%s",
                                         av_get_media_type_string(type),
                                         av_err2str(*stream_index)));
    }

    AVStream *stream = format_context_->streams[*stream_index];

    if (stream == nullptr) {
        throw std::runtime_error("Find video stream failure.");
    }

    const AVCodec *codec = avcodec_find_decoder(stream->codecpar->codec_id);
    if (codec == nullptr) {
        throw std::runtime_error("Find video codec failure.");
    }
    *codec_context = avcodec_alloc_context3(codec);
    if (avcodec_parameters_to_context(*codec_context, stream->codecpar) < 0) {
        throw std::runtime_error("Fill parameters failure.");
    }
    if (avcodec_open2(*codec_context, codec, nullptr) < 0) {
        throw std::runtime_error("Open avcodec failure.");
    }

}

void Demuxing::AllocImage() {

    this->image_dst_buffer_size = av_image_alloc(this->image_data_,
                                                 this->image_data_line_size_,
                                                 this->video_codec_context_->width,
                                                 this->video_codec_context_->height,
                                                 this->video_codec_context_->pix_fmt,
                                                 1);
    if (this->image_dst_buffer_size < 0) {
        throw std::runtime_error(vformat("Alloc image error,message:%s", av_err2str(this->image_dst_buffer_size)));
    }
}
```
### 调用
```cpp
#include "Demuxing.h"
int main(int argc, const char *argv[]) {

    Demuxing demuxing;
    const char *input_mp4= "../res/captain_women.mp4";
    const char *output_pcm= "./audio.pcm";
    const char *output_yuv= "./video.yuv";
    demuxing.Initialize(input_mp4,output_yuv,output_pcm);
    demuxing.Start();

    // ffplay -f f32le -ac 1 -ar 44100 ./cmake-build-debug/audio.pcm
    // ffplay -f rawvideo -pix_fmt yuv420p -video_size 1000x416 ./cmake-build-debug/video.yuv
    return 0;
}
```

## 播放音频pcm文件
  ffplay -f f32le -ac 1 -ar 44100 ./cmake-build-debug/audio.pcm
## 播放视频yuv文件
 ffplay -f rawvideo -pix_fmt yuv420p -video_size 1000x416 ./cmake-build-debug/video.yuv


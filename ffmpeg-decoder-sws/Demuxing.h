//
// Created by blueberry on 2022/1/27.
//

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

    int audio_frame_counter_ = 0;
    int video_frame_counter_ = 1;

    uint8_t *src_image_data_[4] = {nullptr};
    int src_image_data_line_size_[4] = {0};
    int src_image_data_buffer_size_ = 0;

    uint8_t *dst_image_data_[4] = {nullptr};
    int dst_image_data_line_size_[4] = {0};
    int dst_image_data_buffer_size_ = 0;

    AVPacket *av_packet_ = nullptr;
    AVFrame *av_frame_ = nullptr;

    SwsContext* video_sws_context_ = nullptr;
    SwrContext* audio_swr_context_ = nullptr;

    int output_channels_ = 2;

    void OpenCodecContext(AVMediaType type, AVCodecContext **codec_context, int *stream_index) const;

    int DecodePacket(AVCodecContext *codec_context);

    int OutputAudioFrame(AVFrame *frame);

    int OutputVideoFrame(AVFrame *frame);

    void AllocSrcImage();

    void AllocDstImage();
};


#endif //FFMPEG_DECODER_DEMUXING_H

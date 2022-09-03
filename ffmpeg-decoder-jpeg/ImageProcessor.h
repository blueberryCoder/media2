//
// Created by blueberry on 2022/8/13.
//

#ifndef FFMPEG_DECODER_JPEG_IMAGEPROCESSOR_H
#define FFMPEG_DECODER_JPEG_IMAGEPROCESSOR_H

#include <string>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>

#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>

#include <libavutil/avutil.h>
#include <libavutil/opt.h>
}

using namespace std;

class ImageProcessor {
public:
    ImageProcessor();

    ~ImageProcessor();

    void setInput(string input);

    void setOutput(string output);

    void setFilterString(string filter_desc);

    void initialize();

    void process();

private:

    void initializeFilterGraph();

    string input_path_;
    string output_path_;
    string filter_desc_;

    AVFormatContext *avformat_ctx_;
    AVCodecContext *avcodec_decoder_ctx_;
    AVCodecContext *avcodec_encoder_ctx_;

    const AVCodec *avcodec_encoder_;

    AVPacket *avpacket_origin_;
    AVFrame *avframe_origin_;
    AVFrame *avframe_dst_;
    AVPacket *avpacket_dst_;

    int width_;
    int height_;

    AVFilterContext *buffer_src_ctx_;
    AVFilterContext *buffer_sink_ctx_;
    AVFilterGraph *filter_graph_;

    AVFilterInOut * filter_inputs_;
    AVFilterInOut * filter_outputs_;

};


#endif //FFMPEG_DECODER_JPEG_IMAGEPROCESSOR_H

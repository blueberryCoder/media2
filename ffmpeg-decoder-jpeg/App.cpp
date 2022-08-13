//
// Created by blueberry on 2022/8/06.
//
// http://guru-coder.blogspot.com/2014/01/in-memory-jpeg-decode-using-ffmpeg.html

extern "C"
{
#include <libavformat/avformat.h>
#include <libavformat/avio.h>
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libavutil/opt.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>

}

#include <iostream>
#include <fstream>
#include <vector>

int compress_2_jpeg(AVFrame *avframe, std::ofstream &strm) {
    // https://www.codeproject.com/Questions/744389/Trying-to-setup-MJPEG-encoder-in-ffmpeg-in-Cpluspl
    auto codec = avcodec_find_encoder_by_name("mjpeg");
    auto codec_ctx = avcodec_alloc_context3(codec);
    codec_ctx->width = avframe->width;
    codec_ctx->height = avframe->height;
    codec_ctx->time_base.num = 1;
    codec_ctx->time_base.den = 1;
    codec_ctx->pix_fmt = AVPixelFormat(avframe->format);
    int result = avcodec_open2(codec_ctx, codec, nullptr);
    if (result < 0) {
        std::cout << "avcodec open2 failed ,code is " << result << "," << strerror(errno) << __LINE__ << std::endl;
    }
    result = avcodec_send_frame(codec_ctx, avframe);
    if (result != 0) {
        switch (result) {
            case AVERROR(EAGAIN) : {
                std::cout << "send compress " << "AVERROR(EAGAIN)" << std::endl;
                break;
            }
            case AVERROR_EOF: {
                std::cout << "send compress " << "AVERROR(EAGAIN)" << std::endl;
            }
            case AVERROR(EINVAL): {
                std::cout << "send compress " << "AVERROR(EINVAL)" << std::endl;
                break;
            }
            case AVERROR(ENOMEM): {
                std::cout << "send compress " << " AVERROR(ENOMEM)" << std::endl;
                break;
            }
            default: {
                std::cout << "send compress " << "other errors" << result << std::endl;
            }
        }
    }
    auto avpacket = av_packet_alloc();
    result = avcodec_receive_packet(codec_ctx, avpacket);
    if (result < 0) {
        std::cout << "avcodec receive packet fail" << std::endl;
    }

    strm.write(reinterpret_cast<const char *>(avpacket->data), avpacket->size);
    av_packet_free(&avpacket);

    return 0;
}

static AVFilterContext *avfilter_output_ctx = nullptr;
static AVFilterContext *avfilter_input_ctx = nullptr;

int init_graph_filters(AVFrame *av_frame) {
    auto filter_buffer = avfilter_get_by_name("buffer");
    auto filter_buffer_sink = avfilter_get_by_name("buffersink");
    auto avfilter_outputs = avfilter_inout_alloc();
    auto avfilter_inputs = avfilter_inout_alloc();
    auto avfilter_graph = avfilter_graph_alloc();
    if (avfilter_inputs == nullptr || avfilter_outputs == nullptr || avfilter_graph == nullptr) {
        std::cout << "no memory " << std::endl;
    }

    char args[512]{};
    snprintf(args, sizeof(args),
             "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             av_frame->width, av_frame->height, av_frame->format,
             1, 1,
//             av_frame->time_base.num, av_frame->time_base.den,
             av_frame->sample_aspect_ratio.num, av_frame->sample_aspect_ratio.den);
    int ret = avfilter_graph_create_filter(&avfilter_input_ctx, filter_buffer, "in", args, nullptr, avfilter_graph);
    if (ret < 0) {
        std::cout << "create in filter failed: " << ret << std::endl;
    }

    ret = avfilter_graph_create_filter(&avfilter_output_ctx, filter_buffer_sink, "out", nullptr, nullptr,
                                       avfilter_graph);

    if (ret < 0) {
        std::cout << "create out filter failed " << ret << std::endl;
    }
    AVPixelFormat pix_fmts[] = {AV_PIX_FMT_YUVJ420P, AV_PIX_FMT_NONE};
    ret = av_opt_set_int_list(avfilter_output_ctx, "pix_fmts", pix_fmts, AV_PIX_FMT_NONE,
                              AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        std::cout << "set params list to buffer sink failed" << std::endl;
    }

    avfilter_outputs->name = av_strdup("in");
    avfilter_outputs->filter_ctx = avfilter_input_ctx;
    avfilter_outputs->pad_idx = 0;
    avfilter_outputs->next = nullptr;

    avfilter_inputs->name = av_strdup("out");
    avfilter_inputs->filter_ctx = avfilter_output_ctx;
    avfilter_inputs->pad_idx = 0;
    avfilter_inputs->next = nullptr;

    ret = avfilter_graph_parse(avfilter_graph, "hue=180:s=1", avfilter_inputs, avfilter_outputs,
                               nullptr);
    if (ret < 0) {
        std::cout << "avfilter graph parse failed" << std::endl;
    }
    ret = avfilter_graph_config(avfilter_graph, nullptr);
    if (ret < 0) {
        std::cout << "avfilter graph config failed" << std::endl;
    }
    // Need to free inputs and outputs
    return 0;
}


int main(int argc, const char *argv[]) {

    auto *avformat_ctx = avformat_alloc_context();
    auto result = avformat_open_input(&avformat_ctx, "../res/lyf1.jpeg", nullptr, nullptr);
    std::ofstream output("../res/output.jpeg");


    if (result != 0) { // 0 is success
        std::cout << "occurs error code is " << result << std::endl;
        return 0;
    }
    auto codec = avcodec_find_decoder(avformat_ctx->streams[0]->codecpar->codec_id);
    if (codec == nullptr) {
        std::cout << "find decoder failure" << std::endl;
        return 0;
    }
    auto codec_ctx = avcodec_alloc_context3(codec);
    avcodec_parameters_to_context(codec_ctx, avformat_ctx->streams[0]->codecpar);
    result = avcodec_open2(codec_ctx, codec, nullptr);
    if (result < 0) {
        std::cout << "avcodec open2 fail" << std::endl;
    }

    auto av_packet = av_packet_alloc();
    auto av_frame = av_frame_alloc();


    while (av_read_frame(avformat_ctx, av_packet) >= 0) {
        int ret = avcodec_send_packet(codec_ctx, av_packet);
        if (ret < 0) {
            std::cout << "send packet fail" << std::endl;
        }
        while (ret >= 0) {
            ret = avcodec_receive_frame(codec_ctx, av_frame);
            std::cout << "format " << AVPixelFormat(av_frame->format) << ",pkt_size" << av_frame->pkt_size <<
                      "linesize" << av_frame->linesize <<
                      "is " << std::boolalpha
                      << static_cast<bool>(AVPixelFormat(av_frame->format) == AV_PIX_FMT_YUVJ420P) << std::endl;
            if (ret >= 0 && av_frame->pkt_size > 0) {
                init_graph_filters(av_frame);
                if (av_buffersrc_add_frame_flags(avfilter_input_ctx, av_frame, AV_BUFFERSRC_FLAG_KEEP_REF) < 0) {
                    std::cout << "Error while feeding the filtergraph" << std::endl;
                }
                auto av_filter_frame = av_frame_alloc();
                int status = 1;
                int i =0;
                while (status >= 0) {
                    status = av_buffersink_get_frame(avfilter_output_ctx, av_filter_frame);

                    std::cout << "get filter frame " << i++ << std::endl;
                }
                compress_2_jpeg(av_filter_frame, output);
//                output.write(reinterpret_cast<const char *>(av_packet->data), av_packet->size);
            }
            av_frame_unref(av_frame);
        }
        av_packet_unref(av_packet);
    }

    return 0;
}

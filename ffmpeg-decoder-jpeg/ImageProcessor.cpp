//
// Created by blueberry on 2022/8/13.
//

#include "ImageProcessor.h"
#include <fstream>


ImageProcessor::ImageProcessor() {}

ImageProcessor::~ImageProcessor() {
    avformat_free_context(avformat_ctx_);

    av_packet_free(&avpacket_origin_);
    av_packet_free(&avpacket_dst_);

    av_frame_free(&avframe_origin_);
    av_frame_free(&avframe_dst_);

    avfilter_inout_free(&filter_inputs_);
    avfilter_inout_free(&filter_outputs_);

    if (buffer_src_ctx_) {
        avfilter_free(buffer_src_ctx_);
    }
    if (buffer_sink_ctx_) {
        avfilter_free(buffer_sink_ctx_);
    }

    avfilter_graph_free(&filter_graph_);
}

void ImageProcessor::setInput(string input) {
    this->input_path_ = std::move(input);
}

void ImageProcessor::setOutput(string output) {
    this->output_path_ = std::move(output);
}

void ImageProcessor::setFilterString(string filter_desc) {
    this->filter_desc_ = std::move(filter_desc);
}

/**
 * Before initialize you should setInput,setOutput,setFilterString first.
 */
void ImageProcessor::initialize() {

    avpacket_origin_ = av_packet_alloc();
    avpacket_dst_ = av_packet_alloc();
    avframe_origin_ = av_frame_alloc();
    avframe_dst_ = av_frame_alloc();

    avformat_ctx_ = avformat_alloc_context();
    //  open format
    int ret = avformat_open_input(&avformat_ctx_, this->input_path_.c_str(), nullptr, nullptr);
    if (ret < 0) {
        throw std::runtime_error("avformat open input error code is : " + std::to_string(ret));
    }
    // init decoder
    const auto codec_decoder = avcodec_find_decoder(avformat_ctx_->streams[0]->codecpar->codec_id);
    if (codec_decoder == nullptr) {
        throw std::runtime_error("avcodec find decoder failed.");
    }
    avcodec_decoder_ctx_ = avcodec_alloc_context3(codec_decoder);
    ret = avcodec_parameters_to_context(avcodec_decoder_ctx_, avformat_ctx_->streams[0]->codecpar);
    if (ret < 0) {
        throw std::runtime_error("avcodec_parameters_to_context failed code is " + std::to_string(ret));
    }
    // read frame
    ret = av_read_frame(avformat_ctx_, avpacket_origin_);
    if (ret < 0) {
        throw std::runtime_error("av read frame " + std::to_string(ret));
    }
    // open decoder
    ret = avcodec_open2(avcodec_decoder_ctx_, codec_decoder, nullptr);
    if (ret < 0) {
        throw std::runtime_error("avcodec open failed. codes is " + std::to_string(ret));
    }
    // decode packet to frame
    ret = avcodec_send_packet(avcodec_decoder_ctx_, avpacket_origin_);
    if (ret < 0) {
        throw std::runtime_error("avcodec send packet failed. code is " + std::to_string(ret));
    }
    ret = avcodec_receive_frame(avcodec_decoder_ctx_, avframe_origin_);
    if (ret < 0) {
        throw std::runtime_error("avcodec_receive_frame failed. code is " + std::to_string(ret));
    }
    this->width_ = avframe_origin_->width;
    this->height_ = avframe_origin_->height;

    // init encoder
    avcodec_encoder_ = avcodec_find_encoder_by_name("mjpeg");
    if (avcodec_encoder_ == nullptr) {
        throw std::runtime_error("find encoder by name failed.");
    }
    avcodec_encoder_ctx_ = avcodec_alloc_context3(avcodec_encoder_);
    if (avcodec_encoder_ctx_ == nullptr) {
        throw std::runtime_error("avcodec alloc failed");
    }
    avcodec_encoder_ctx_->width = this->width_;
    avcodec_encoder_ctx_->height = this->height_;
    avcodec_encoder_ctx_->pix_fmt = AVPixelFormat(avframe_origin_->format);
    avcodec_encoder_ctx_->time_base = {1, 1};

    initializeFilterGraph();
}

void ImageProcessor::process() {
    // send to filter graph.
    int ret = av_buffersrc_add_frame(this->buffer_src_ctx_, this->avframe_origin_);
    if (ret < 0) {
        throw std::runtime_error("add frame to filter failed code is " + std::to_string(ret));
    }
    ret = av_buffersink_get_frame(this->buffer_sink_ctx_, this->avframe_dst_);
    if (ret < 0) {
        throw std::runtime_error("get avframe from sink failed code is " + std::to_string(ret));
    }

    ret = avcodec_open2(avcodec_encoder_ctx_, avcodec_encoder_, nullptr);
    if (ret < 0) {
        throw std::runtime_error("open encode failed code is " + std::to_string(ret));
    }

    ret = avcodec_send_frame(avcodec_encoder_ctx_, this->avframe_dst_);
    if (ret < 0) {
        throw std::runtime_error("encoder failed,code is " + std::to_string(ret));
    }
    ret = avcodec_receive_packet(avcodec_encoder_ctx_, avpacket_dst_);
    if (ret < 0) {
        throw std::runtime_error("avcodec receive packet failed code is " + std::to_string(ret));
    }

    std::ofstream output_file(this->output_path_);
    output_file.write(reinterpret_cast<const char *>(this->avpacket_dst_->data), avpacket_dst_->size);
}

void ImageProcessor::initializeFilterGraph() {
    // 创建滤镜图实例
    this->filter_graph_ = avfilter_graph_alloc();
    // 获取输入滤镜的定义
    const auto avfilter_buffer_src = avfilter_get_by_name("buffer");
    if (avfilter_buffer_src == nullptr) {
        throw std::runtime_error("buffer avfilter get failed.");
    }
    // 获取输出滤镜的定义
    const auto avfilter_buffer_sink = avfilter_get_by_name("buffersink");
    if (avfilter_buffer_sink == nullptr) {
        throw std::runtime_error("buffersink avfilter get failed.");
    }
    // 创建输入实体
    this->filter_inputs_ = avfilter_inout_alloc();
    // 创建输出实体
    this->filter_outputs_ = avfilter_inout_alloc();

    if (this->filter_inputs_ == nullptr || this->filter_outputs_ == nullptr) {
        throw std::runtime_error("filter inputs is null");
    }

    char args[512]{};
    snprintf(args, sizeof(args), "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             this->width_,
             this->height_,
             1,
             1,
             avframe_origin_->format,
             avframe_origin_->sample_aspect_ratio.num,
             avframe_origin_->sample_aspect_ratio.den);
    // 根据参数创建输入滤镜的上下文，并添加到滤镜图
    int ret = avfilter_graph_create_filter(&this->buffer_src_ctx_, avfilter_buffer_src, "in",
                                           args, nullptr, this->filter_graph_);
    if (ret < 0) {
        throw std::runtime_error("create filter failed .code is " + std::to_string(ret));
    }
    // 根据参数创建输出滤镜的上下文，并添加到滤镜图
    ret = avfilter_graph_create_filter(&this->buffer_sink_ctx_, avfilter_buffer_sink, "out", nullptr,
                                       nullptr, this->filter_graph_);
    if (ret < 0) {
        throw std::runtime_error("create filter failed code is " + std::to_string(ret));
    }
    AVPixelFormat pix_fmts[] = {AV_PIX_FMT_YUVJ420P, AV_PIX_FMT_NONE};
    ret = av_opt_set_int_list(buffer_sink_ctx_, "pix_fmts", pix_fmts, AV_PIX_FMT_NONE,
                              AV_OPT_SEARCH_CHILDREN);
    // 为输入滤镜关联滤镜名，滤镜上下文
    this->filter_outputs_->name = av_strdup("in");
    this->filter_outputs_->filter_ctx = this->buffer_src_ctx_;
    this->filter_outputs_->pad_idx = 0;
    this->filter_outputs_->next = nullptr;
    // 为输出滤镜关联滤镜名，滤镜上下文
    this->filter_inputs_->name = av_strdup("out");
    this->filter_inputs_->filter_ctx = this->buffer_sink_ctx_;
    this->filter_inputs_->pad_idx = 0;
    this->filter_inputs_->next = nullptr;
    // 根据传入的字符串解析出滤镜，并加入到滤镜图
    ret = avfilter_graph_parse_ptr(this->filter_graph_,
                               this->filter_desc_.c_str(),
                               &this->filter_inputs_,
                               &this->filter_outputs_,
                               nullptr);
    if (ret < 0) {
        throw std::runtime_error("avfilter graph parse failed .code is " + std::to_string(ret));
    }
    ret = avfilter_graph_config(this->filter_graph_, nullptr);
    if (ret < 0) {
        throw std::runtime_error("avfilter graph config failed.");
    }
}
//
// Created by blueberry on 2022/1/27.
//

#include "Demuxing.h"

#include "string_util.h"
#include <ios>

Demuxing::Demuxing() = default;

Demuxing::~Demuxing() {

    av_freep(this->src_image_data_);
    av_freep(this->dst_image_data_);

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

    // 创建SwsContext,把原视频尺寸缩小一半
    this->video_sws_context_ = sws_getContext(
            this->video_codec_context_->width,
            this->video_codec_context_->height,
            this->video_codec_context_->pix_fmt,
            this->video_codec_context_->width / 2,
            this->video_codec_context_->height / 2,
            this->video_codec_context_->pix_fmt,
            SWS_FAST_BILINEAR,
            nullptr, nullptr, nullptr);

    if (this->video_codec_context_ == nullptr) {
        throw std::runtime_error("sws_getContext fail.");
    }
    // 1. 设置通用参数
    this->audio_swr_context_ = swr_alloc_set_opts(nullptr,
                                                  av_get_default_channel_layout(output_channels_),
                                                  AV_SAMPLE_FMT_S16,
                                                  audio_codec_context_->sample_rate,
                                                  audio_codec_context_->channel_layout,
                                                  audio_codec_context_->sample_fmt,
                                                  audio_codec_context_->sample_rate,
                                                  0,
                                                  nullptr
    );
    if (this->audio_swr_context_ == nullptr) {
        throw std::runtime_error("audio_swr_context fail.");
    }
    // 2. 初始化
    swr_init(this->audio_swr_context_);

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
    AllocSrcImage();
    AllocDstImage();
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
    if (this->audio_swr_context_) {
        // https://ffmpeg.org/doxygen/2.4/resampling__audio_8c_source.html

        uint8_t **audio_dst_data = nullptr;
        int linesize = 0;
        // 为输出分配缓存
        av_samples_alloc_array_and_samples(&audio_dst_data, &linesize, output_channels_,
                                           frame->nb_samples, AV_SAMPLE_FMT_S16, 1);
        // 转换
        int num_frames = swr_convert(audio_swr_context_,
                                     audio_dst_data,
                                     frame->nb_samples,
                                     const_cast<const uint8_t **>(frame->data),
                                     frame->nb_samples);

        int unpadded_line_size = num_frames * av_get_bytes_per_sample(AV_SAMPLE_FMT_S16) * output_channels_;
        std::cout << vformat("Write audio frame %d,size=%d", this->audio_frame_counter_++, unpadded_line_size)
                  << std::endl;
        output_audio_stream_.write(reinterpret_cast<const char *>(audio_dst_data[0]), unpadded_line_size);
        // 释放缓存
        if (&audio_dst_data[0]) {
            av_freep(&audio_dst_data[0]);
        }
        av_freep(&audio_dst_data);
        // ffplay -showmode 1 -f s16le -channels 2  -ar 44100 cmake-build-debug/audio.pcm
    } else {
        int unpadded_line_size = frame->nb_samples * av_get_bytes_per_sample(AVSampleFormat(frame->format));
        std::cout << vformat("Write audio frame %d,size=%d", this->audio_frame_counter_++, unpadded_line_size)
                  << std::endl;
        // flt32格式的话：frame->extended_data[0] == frame->data[0]
        for (int i = 0; i < unpadded_line_size; i += 4) {
            output_audio_stream_.write(reinterpret_cast<const char *>(frame->data[0] + i), 4);
            output_audio_stream_.write(reinterpret_cast<const char *>(frame->data[1] + i), 4);
        }
    }
    return 1;
}


int Demuxing::OutputVideoFrame(AVFrame *frame) {
    if (frame->width != this->video_codec_context_->width
        || frame->height != this->video_codec_context_->height
        || frame->format != this->video_codec_context_->pix_fmt) {
        throw std::runtime_error("The video frame width,height and fmt must same .");
    }
    av_image_copy(src_image_data_,
                  src_image_data_line_size_,
                  const_cast<const uint8_t ** > ( reinterpret_cast< uint8_t **>(frame->data)),
                  frame->linesize,
                  this->video_codec_context_->pix_fmt,
                  frame->width,
                  frame->height);

    if (this->video_sws_context_) {
       int h = sws_scale(this->video_sws_context_,src_image_data_,src_image_data_line_size_,0,frame->height,
        dst_image_data_,dst_image_data_line_size_);
        output_video_stream_.write(reinterpret_cast<const char *>(this->dst_image_data_[0]), this->dst_image_data_buffer_size_);
        std::cout << vformat("sws_scale return %d",h) << std::endl;
    } else {
        std::cout << vformat("Write video frame %d,size=%d,width=%d,height=%d,fmt=%s",
                             this->video_frame_counter_++,
                             this->src_image_data_buffer_size_,
                             frame->width,
                             frame->height,
                             av_get_pix_fmt_name(AVPixelFormat(frame->format)))
                  << std::endl;
//        output_video_stream_.write(reinterpret_cast<const char *>(this->src_image_data_[0]), this->src_image_data_buffer_size_  );
        output_video_stream_.write(reinterpret_cast<const char *>(this->src_image_data_[0]), frame->width * frame->height );
        output_video_stream_.write(reinterpret_cast<const char *>(this->src_image_data_[1]), frame->width * frame->height / 4 );
        output_video_stream_.write(reinterpret_cast<const char *>(this->src_image_data_[2]), frame->width * frame->height / 4 );
    }
    return 1;
}

/**
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

void Demuxing::AllocSrcImage() {
    this->src_image_data_buffer_size_ = av_image_alloc(this->src_image_data_,
                                                       this->src_image_data_line_size_,
                                                       this->video_codec_context_->width,
                                                       this->video_codec_context_->height,
                                                       this->video_codec_context_->pix_fmt,
                                                       1);
    if (this->src_image_data_buffer_size_ < 0) {
        throw std::runtime_error(vformat("Alloc image error,message:%s", av_err2str(this->src_image_data_buffer_size_)));
    }
}
void Demuxing::AllocDstImage() {
    this->dst_image_data_buffer_size_= av_image_alloc(this->dst_image_data_,
                                                      this->dst_image_data_line_size_,
                                                      this->video_codec_context_->width /2,
                                                      this->video_codec_context_->height /2,
                                                      this->video_codec_context_->pix_fmt,
                                                      1);
    if (this->dst_image_data_buffer_size_ < 0) {
        throw std::runtime_error(vformat("Alloc image error,message:%s", av_err2str(this->src_image_data_buffer_size_)));
    }
}
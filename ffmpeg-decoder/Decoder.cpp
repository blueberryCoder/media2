#include <iostream>
#include <fstream>
#include <stdint.h>
#include <vector>

extern "C"
{
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <libavutil/pixdesc.h>
#include <libavutil/error.h>
#include <libavutil/imgutils.h>
#include <libavcodec/avcodec.h>
#include <libavutil/timestamp.h>
}
#include "string_util.h"
#define OUT_PUT_CHANNELS 2

// https://blog.csdn.net/ydjjcdd/article/details/109448724
// https://github.com/zhanxiaokai/Android-FFmpegDecoder/blob/master/jni/decoder/libffmpeg_decoder/accompany_decoder.cpp
static int width, height;
static AVPixelFormat pix_fmt;
static int video_dst_bufsize;
static uint8_t *videoDstData[4] = {nullptr};
static int videoDstLineSizes[4];
static int videoFrameCount = 0;
static int audioFrameCount = 0;
static std::fstream outputAudioStream;
static std::fstream outputVideoStream;


void allocImage(const AVCodecContext *videoCodecCtx);


static int getFormatFromSampleFmt(const char **fmt, AVSampleFormat sampleFormat) {
    struct SampleFmtEntry {
        AVSampleFormat sample_format;
        const char *fmt_be, *fmt_le;
    };
    SampleFmtEntry sample_fmt_entries[] = {
            {AV_SAMPLE_FMT_U8,  "u8",    "u8"},
            {AV_SAMPLE_FMT_S16, "s16be", "s16le"},
            {AV_SAMPLE_FMT_S32, "s32be", "s32le"},
            {AV_SAMPLE_FMT_FLT, "f32be", "f32le"},
            {AV_SAMPLE_FMT_DBL, "f64be", "f64le"},
    };
    *fmt = nullptr;
    for (int i = 0; i < FF_ARRAY_ELEMS(sample_fmt_entries); i++) {
        SampleFmtEntry *entry = &sample_fmt_entries[i];
        if (sampleFormat == entry->sample_format) {
            *fmt = AV_NE(entry->fmt_be, entry->fmt_le);
            return 0;
        }
    }
    std::cout << vformat("sample format %s is not supported as out format \n", av_get_sample_fmt_name(sampleFormat));
    return -1;
}

static int outputVideoFrame(AVFrame *frame) {
    if (frame->width != width || frame->height != height || frame->format != pix_fmt) {
        throw vformat("Error: width,height and pixel format have to be constant in a rawvideo but the width ,height or "
                      "pixel format of the input video changed:", width, height, pix_fmt);
        return -1;
    }
    std::cout << "video_frame" << videoFrameCount++ << std::endl;
    av_image_copy(videoDstData, videoDstLineSizes,
                  const_cast <const uint8_t **>(reinterpret_cast< uint8_t **>(frame->data)),
                  frame->linesize, pix_fmt, width, height);
    outputVideoStream.write(reinterpret_cast<const char *>(videoDstData[0]), video_dst_bufsize);
    return 0;
}

static int outputAudioFrame(AVFrame *frame) {
    size_t unpaddedLineSize = frame->nb_samples * av_get_bytes_per_sample(AVSampleFormat(frame->format));
    std::cout << vformat("audio frame n:%d nb_samples:%d \n", audioFrameCount++, frame->nb_samples) << std::endl;
    outputAudioStream.write(reinterpret_cast<const char *>(frame->extended_data[0]), unpaddedLineSize);
    return 0;
}

/**
 * @brief 获取解码器信息以及流的index
 * 
 * @param formatCtx 
 * @param type 流类型，如音频或视频
 * @param streamIndex 流的位置
 * @param codecContext 解码器上下文
 * @return int 
 */
static int openCodecContext(AVFormatContext *formatCtx, AVMediaType type,
                            int *streamIndex, AVCodecContext **codecContext) {
    // 如果成功返回，ret就是stream index
    int ret = av_find_best_stream(formatCtx, type, -1, -1, nullptr, 0);
    if (ret < 0) {
        char buf[500] = {};
        snprintf(buf, sizeof(buf), "Find the bes stream error.meida type= %s", av_get_media_type_string(type));
        throw buf;
    } else {
        int streamIndexCandidate = ret;
        AVStream *stream = formatCtx->streams[streamIndexCandidate];
        // find decoder
        const AVCodec *dec = avcodec_find_decoder(stream->codecpar->codec_id);
        if (!dec) {
            throw "Find the codec failed.";
        }
        *codecContext = avcodec_alloc_context3(dec);
        if (*codecContext == nullptr) {
            throw "avcodec allock fialed";
        }
        ret = avcodec_parameters_to_context(*codecContext, stream->codecpar);
        if (ret < 0) {
            throw "Fill parameter to context is fail.";
        }
        ret = avcodec_open2(*codecContext, dec, nullptr);
        if (ret < 0) {
            throw "Avcodec open fail.";
        }
        *streamIndex = streamIndexCandidate;
        return 0;
    }
}


static int decodePacket(AVCodecContext *dec, const AVPacket *pkt, AVFrame *frame) {
    int ret = 0;
    ret = avcodec_send_packet(dec, pkt);
    if (ret < 0) {
        std::string str = vformat("Error summitting a packet for decoding (%s)\n", av_err2str(ret));
        std::cout << str << std::endl;
        return ret;
    }
    while (ret >= 0) {
        ret = avcodec_receive_frame(dec, frame);
        if (ret < 0) {
            if (ret == AVERROR_EOF || ret == AVERROR(EAGAIN)) {
                return 0;
            }
            std::cout << vformat("Error during decoding (%s)\n", av_err2str(ret));
            return ret;
        }
        if (dec->codec->type == AVMEDIA_TYPE_VIDEO) {
            ret = outputVideoFrame(frame);
        } else {
            ret = outputAudioFrame(frame);
        }
        av_frame_unref(frame);
        if (ret < 0) {
            return ret;
        }
    }
    return 0;
}


int main(int argc, const char *argv[]) {
    using namespace std;
    // Step1, 注册
    avformat_network_init();
    // av_register_all();
    AVFormatContext *formatCtx = avformat_alloc_context();
    const char *inputMp4 = "../res/captain_women.mp4";
    const char *outputPcm = "./audio.pcm";
    const char *outputYuv = "./video.yuv";
    outputAudioStream.open(outputPcm, ios::binary | ios::out);
    if (outputAudioStream.bad()) {
        throw std::runtime_error("Open the pcm output file failed.");
    }
    outputVideoStream.open(outputYuv, ios::binary | ios::out);
    if (outputVideoStream.bad()) {
        throw "Open the yuv output file failed.";
    }

    if (avformat_open_input(&formatCtx, inputMp4, nullptr, nullptr) < 0) {
        throw "Open input mp4 failed.";
    }

    if (avformat_find_stream_info(formatCtx, nullptr) < 0) {
        throw "Find stream info is failed.";
    }
    // Step3, 寻找流
    // Find the audio index and the video index .
    int videoStreamIndex = -1;
    int audioStreamIndex = -1;

    AVCodecContext *videoCodecCtx, *audioCodecCtx;
    openCodecContext(formatCtx, AVMEDIA_TYPE_VIDEO, &videoStreamIndex, &videoCodecCtx);
    AVStream *videoStream = formatCtx->streams[videoStreamIndex];

    allocImage(videoCodecCtx);
    openCodecContext(formatCtx, AVMEDIA_TYPE_AUDIO, &audioStreamIndex, &audioCodecCtx);
    AVStream *audioStream = formatCtx->streams[audioStreamIndex];
    av_dump_format(formatCtx, 0, inputMp4, 0);

    AVFrame *frame = av_frame_alloc();
    if (!frame) {
        throw "Frame alloc failed.";
    }

    AVPacket *pkt = av_packet_alloc();
    if (!pkt) {
        throw "Packet alloc failed.";
    }
    std::cout << vformat("Demuxing video from '%s' into '%s' \n", inputMp4, outputYuv) << std::endl;
    std::cout << vformat("Demuxing audio from '%s' into '%s' \n", inputMp4, outputPcm) << std::endl;
    int ret = 0;
    while (av_read_frame(formatCtx, pkt) >= 0) {
        if (pkt->stream_index == videoStreamIndex) {
            ret = decodePacket(videoCodecCtx, pkt, frame);
        } else if (pkt->stream_index == audioStreamIndex) {
            ret = decodePacket(audioCodecCtx, pkt, frame);
        }
        av_packet_unref(pkt);
        if (ret < 0) {
            break;
        }
    }
    if (videoCodecCtx) {
        decodePacket(videoCodecCtx, nullptr, frame);
    }
    if (audioCodecCtx) {
        decodePacket(audioCodecCtx, nullptr, frame);
    }

    std::cout << "Demuxing succeeded." << std::endl;
    if (videoStream) {
        std::cout << vformat("Play the output video file with the command :\n"
                             "ffplay -f rawvideo -pix_fmt %s -video_size %dx%d %s \n",
                             av_get_pix_fmt_name(pix_fmt), width, height, outputYuv
        ) << std::endl;
    }

    if (audioStream) {
        AVSampleFormat sfmt = audioCodecCtx->sample_fmt;
        int n_channels = audioCodecCtx->channels;
        if (av_sample_fmt_is_planar(sfmt)) {
            const char *packed = av_get_sample_fmt_name(sfmt);
            std::cout << vformat("Waring: the sample format the decoder produced is planar "
                                 "(%s). This example will output the first channel only.\n",
                                 packed ? packed : ""
            );
            sfmt = av_get_packed_sample_fmt(sfmt);
            n_channels = 1;
        }
        const char *fmt;
        if (getFormatFromSampleFmt(&fmt, sfmt) < 0) {
            goto end;
        }

        std::cout << vformat("Play the output audio file with the command :\n"
                             "ffplay -f %s -ac %d -ar %d %s \n",
                             fmt, n_channels, audioCodecCtx->sample_rate, outputPcm);
    }

    end:
    avcodec_free_context(&videoCodecCtx);
    avcodec_free_context(&audioCodecCtx);
    avformat_close_input(&formatCtx);
    outputAudioStream.close();
    outputVideoStream.close();
    av_packet_free(&pkt);
    av_frame_free(&frame);
    av_free(videoDstData[0]);
    return ret < 0;
}

void allocImage(const AVCodecContext *videoCodecCtx) {
    width = videoCodecCtx->width;
    height = videoCodecCtx->height;
    pix_fmt = videoCodecCtx->pix_fmt;
    video_dst_bufsize = av_image_alloc(videoDstData, videoDstLineSizes, width, height, pix_fmt, 1);
    if (video_dst_bufsize < 0) {
        throw "Could not allocate new raw video buff.";
    }
}

//
// Created by blueberry on 2022/1/29.
//

#include "Demuxing.h"
int main(int argc, const char *argv[]) {

    Demuxing demuxing;
//    const char *input_mp4= "../res/captain_women.mp4";
    const char *input_mp4= "../res/japan.mp4";
    const char *output_pcm= "./audio.pcm";
    const char *output_yuv= "./video.yuv";
    demuxing.Initialize(input_mp4,output_yuv,output_pcm);
    demuxing.Start();

    // ffplay -f f32le -ac 1 -ar 44100 ./cmake-build-debug/audio.pcm
    // 双声道：ffplay -showmode 1 -f f32le -ac 2 -ar 44100 ./cmake-build-debug/audio.pcm
    // ffplay -f rawvideo -pix_fmt yuv420p -video_size 1000x416 ./cmake-build-debug/video.yuv
    return 0;
}

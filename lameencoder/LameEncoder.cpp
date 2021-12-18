#include <iostream>
#include "Mp3Encoder.h"

int main(int argc, const char *argv[])
{
    Mp3Encoder encoder;
    // 441000 * 16 * 2 = 1,411,200
    // 压缩10
    encoder.Init("../res/vocal.pcm", "./vocalmp3.mp3", 44100, 2, 14112);
    encoder.Encode();
    encoder.Destroy();
    return 0;
}
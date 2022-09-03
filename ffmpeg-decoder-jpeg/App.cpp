//
// Created by blueberry on 2022/8/06.
//
// http://guru-coder.blogspot.com/2014/01/in-memory-jpeg-decode-using-ffmpeg.html
#include "ImageProcessor.h"

int main(int argc, const char *argv[]) {
    ImageProcessor processor;
    processor.setInput("../res/lyf1.jpeg");
    processor.setOutput("../res/output.jpeg");
    // 根据想要的效果传入滤镜描述字符串
    processor.setFilterString("hue=120:s=1");
//    processor.setFilterString("split [main][tmp]; [tmp] crop=iw:ih/2:0:0,vflip [flip]; [main][flip] overlay=0:H/2");
    processor.initialize();
    processor.process();
    return 0;
}

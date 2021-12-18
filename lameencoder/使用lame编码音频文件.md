## 资源准备

[https://lame.sourceforge.io/download.php](https://lame.sourceforge.io/download.php])
[https://sourceforge.net/projects/lame/files/lame/](https://sourceforge.net/projects/lame/files/lame/)
下载：lame-3.100

## 编译lame3

在现在的源码根目录下

1. 配置

```bash
./configure --host="x86_64"
```
2. 编译

``` bash
make
```

3. 安装
``` bash
make install
```

默认会被安装在`/usr/local`目录下。lame安装之后并不存在pkgconfig配置，所有我们使用CMakeLists.txt使用时需要自己制定头文件目录已经链接库的目录以及链接库名

## CMakeLists.txt
```cmake
cmake_minimum_required(VERSION 3.14)
project(Lame_Encoder)
set(CMAKE_CXX_STANDARD 11)            # Enable c++11 standard

set(CMAKE_BUILD_TYPE Debug)
set(SOURCE_FILES LameEncoder.cpp
    Mp3Encoder.h
    Mp3Encoder.cpp
)

add_executable(Lame_Encoder ${SOURCE_FILES})
target_include_directories( ${PROJECT_NAME} PRIVATE "/usr/local/include")
target_link_directories(${PROJECT_NAME} PRIVATE "/usr/local/lib")
target_link_libraries(${PROJECT_NAME} PRIVATE "mp3lame")
```

## 封装Mp3Encoder

Mp3Encoder.h

```c++
#ifndef MP3_ENCODER
#define MP3_ENCODER

#include <stdio.h>
#include <lame/lame.h>

class Mp3Encoder
{
private:
    FILE* pcmFile;
    FILE* mp3File;
    lame_t lameClient;

public:
    Mp3Encoder(/* args */) {}
    virtual ~Mp3Encoder(){}
    int Init(const char* pcmFilePath,const char* mp3FilePath,int sampleRate,
    int channels,int bitRate);
    void Encode();
    void Destroy();
};

#endif /* MP3_ENCODER */
```
Mp3Encoder.cpp

```c++
#include "Mp3Encoder.h"
int Mp3Encoder::Init(const char *pcmFilePath, const char *mp3FilePath, int sampleRate,
                     int channels, int bitRate)
{
    int ref = -1;
    pcmFile = fopen(pcmFilePath, "rb");
    if (!pcmFile)
    {
        return -1;
    }
    mp3File = fopen(mp3FilePath, "wb");
    if (!mp3File)
    {
        return -1;
    }

    // 初始化lame
    lameClient = lame_init();
    lame_set_in_samplerate(lameClient, sampleRate);
    lame_set_out_samplerate(lameClient, sampleRate);
    lame_set_num_channels(lameClient, channels);
    lame_set_brate(lameClient, bitRate / 1000);
    lame_init_params(lameClient);

    return 0;
}

void Mp3Encoder::Encode()
{
    int bufferSize = 1024 * 256;
    short *buffer = new short[bufferSize / 2];
    short *leftBuffer = new short[bufferSize / 4];
    short *rightBuffer = new short[bufferSize / 4];
    unsigned char *mp3_buffer = new unsigned char[bufferSize];
    size_t readBufferSize = 0;
    while ((readBufferSize = fread(buffer, 2, bufferSize / 2, pcmFile)) > 0)
    {
        for (int i = 0; i < readBufferSize; i++)
        {
            if (i % 2 == 0)
            {
                leftBuffer[i / 2] = buffer[i];
            }
            else
            {
                rightBuffer[i / 2] = buffer[i];
            }
        }
        size_t wroteSize = lame_encode_buffer(lameClient, (short int *)leftBuffer,
                                              (short int *)rightBuffer, (int)(readBufferSize / 2), mp3_buffer, bufferSize);
        fwrite(mp3_buffer, 1, wroteSize, mp3File);
    }
    delete[] buffer;
    delete[] leftBuffer;
    delete[] rightBuffer;
    delete[] mp3_buffer;
}

void Mp3Encoder::Destroy(){
    if(pcmFile){
        fclose(pcmFile);
    }
}
```

使用
LameEncoder.cpp

```c++
#include <iostream>
#include "Mp3Encoder.h"

int main(int argc, const char *argv[])
{
    Mp3Encoder encoder;
    // 441000 * 16 * 2 = 1,411,200
    encoder.Init("../res/vocal.pcm", "./vocalmp3.mp3", 44100, 2, 14112);
    encoder.Encode();
    encoder.Destroy();
    return 0;
}
```

## ffplay
另外如果安装了ffmpeg可是使用ffplay测试自己的pcm原始文件

```bash
ffplay -channels 2 -f s16le -i ./vocal.pcm
```

## 引用

https://lame.sourceforge.io/download.php
https://sourceforge.net/projects/lame/files/lame/
https://www.cnblogs.com/renhui/p/12148330.html
https://blog.csdn.net/leixiaohua1020/article/details/50534316
# 使用X265编码视频

## 环境准备
1. 使用hg 下载x264源码
[https://www.videolan.org/developers/x265.html](https://www.videolan.org/developers/x265.html)

2. 如果电脑之前没有安装过 hg,yasm,nasm 可以使用 brew 安装一下
``` bash
    brew install hg
    brew install yasm
    brew install nasm
```
3. 进入到工程 ../x265/build/linux 目录下编译
即便是Mac,也可以在这个目录下进行编译（我使用xcode编译，在安装阶段报错找不到静态包，所以使用在linux目录下编译）

```bash
sh make-Makefiles.bash
```
可以将其中的 `ENABLE_SHARED` 开关打开
配置完成之后
```bash
make
make install
```

## 命令行使用
如
```bash
 x265 -o akiyo.265 --fps=24 --input-res=352x288 --input-csp=i420 --input-depth=8 akiyo_cif.yuv
```
更多使用方式可以参考
```bash
x265 --help
```
## 工程使用
我使用的是vscode + cmake
因为安装的时候将pkgconfig 安装到了这个路径
/usr/local/lib/pkgconfig/x265


同时使用
pkg-config 命令确保,pkgconfig能够搜索到
```bash
pkg-config --list-all | grep x265
x265                                x265 - H.265/HEVC video encoder
```
如搜索不到，可配置环境变量试试
```bash
export PKG_CONFIG_PATH=/usr/local/lib/pkgconfig
```
pkgconfig 可以帮助我们简化依赖

1. 资源准备

yuv 视频源： http://trace.eas.asu.edu/yuv/index.html
查看YUV视频源工具： https://files.cnblogs.com/littlejohnny/YUVviewer_src.rar
https://github.com/IENT/YUView/releases

我下载了一个资源名,名称为：akiyo_cif,是一个I420,8位深的资源

2. CMakeList.txt

```cmake
cmake_minimum_required(VERSION 3.14)
project(x265_encoder)
set(CMAKE_CXX_STANDARD 11)            # Enable c++11 standard

set(CMAKE_BUILD_TYPE Debug)
set(SOURCE_FILES X265Encoder.cpp)

# https://cmake.org/cmake/help/latest/module/FindPkgConfig.html
# https://zhuanlan.zhihu.com/p/111155574
find_package(PkgConfig REQUIRED)
pkg_check_modules(x265 REQUIRED IMPORTED_TARGET x265 )


add_executable(x265_encoder ${SOURCE_FILES})

target_link_libraries(${PROJECT_NAME} PRIVATE PkgConfig::x265)
```

3. 代码
```cpp
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <iomanip>
#include <x265.h>

// https://blog.csdn.net/leixiaohua1020/article/details/42079101
int main(int argv, const char *args[])
{
    FILE *fp_src = fopen("res/akiyo_cif.yuv", "rb");
    FILE *fp_dst = fopen("res/akiyo_cif_code.h265", "wb");

    // 352x288
    int width = 352;
    int height = 288;
    int csp = X265_CSP_I420;
    // 设置为0，会根据文件大小计算帧数
    // 如果帧数设置的太小，可能会造成没有编码后的结果
    int frameNum = 0;

    if (fp_src && fp_dst)
    {
        x265_param *pParam = x265_param_alloc();
        // 设置参数
        x265_param_default(pParam);
        pParam->bRepeatHeaders = 1;
        pParam->internalCsp = csp;
        pParam->sourceWidth = width;
        pParam->sourceHeight = height;
        pParam->fpsNum = 24;
        pParam->fpsDenom = 1;

        // 打开编码器
        x265_encoder *pHander = x265_encoder_open(pParam);
        if (pHander == nullptr)
        {
            std::cout << "open encoder error." << std::endl;
            return -1;
        }
        int ySize = width * height;
        // 根据资源的大小设置的尺寸
        x265_picture *pPic_in = x265_picture_alloc();
        x265_picture_init(pParam, pPic_in);
        char * buff = nullptr;
        switch (csp)
        {
        case X265_CSP_I420:
            buff = new char[ySize * 3];
            pPic_in -> planes[0] = buff;
            pPic_in -> planes[1] = buff + ySize;
            pPic_in -> planes[2] = buff + ySize* 5 /4;
            pPic_in -> stride[0] = width;
            pPic_in -> stride[1] = width/2;
            pPic_in -> stride[2] = width/2;
            break;
        
        default:
            break;
        }
        // 计算帧数
        if (frameNum == 0)
        {
            switch (csp)
            {
            case X265_CSP_I420:
                fseek(fp_src, 0, SEEK_END);
                frameNum = ftell(fp_src) / (ySize * 3 / 2);
                fseek(fp_src, 0, SEEK_SET);
                break;

            default:
                std::cout << "Colorespace are not support." << std::endl;
                return -1;
            }
        }

        x265_nal *pNals = nullptr;
        uint32_t iNal = 0;
        // Loop to encode
        for (int i = 0; i < frameNum; i++)
        {
            switch (csp)
            {
            case X265_CSP_I420:
                // https://www.cplusplus.com/reference/cstdio/fseek/
                fread(pPic_in->planes[0], ySize, 1, fp_src);     // Y
                fread(pPic_in->planes[1], ySize / 4, 1, fp_src); // U
                fread(pPic_in->planes[2], ySize / 4, 1, fp_src); // V
                break;

            default:
                std::cout << "Coloresapce are not support." << std::endl;
                return -1;
            }
            // PTS VS DTS
            // https://bitmovin.com/docs/encoding/faqs/why-is-an-timestamp-offset-for-ts-muxings-applied-by-default
            // pPic_in->i_pts = i;

            int ret = x265_encoder_encode(pHander, &pNals, &iNal, pPic_in,nullptr);
            if (ret < 0)
            {
                std::cout << "Error encode." << std::endl;
                return -1;
            }
            std::cout << "success encode frame" << std::setw(5) << i << std::endl;
            for (int j = 0; j < iNal; j++)
            {
                std::cout << "write nal to file" << std::setw(5) << j << std::endl;
                fwrite(pNals[j].payload, 1, pNals[j].sizeBytes, fp_dst);
            }
        }
        std::cout << "to flush" << std::endl;
        // flush
        while (true)
        {
            int ret = x265_encoder_encode(pHander, &pNals, &iNal, pPic_in,nullptr);
            if (ret == 0)
            {
                break;
            }
            else if (ret < 0)
            {
                std::cout << "Error encode ." << std::endl;
                break;
            }
            else
            {
                std::cout << "Flush 1 frame." << std::endl;
                for (int j = 0; j < iNal; j++)
                {
                    fwrite(pNals[j].payload, 1, pNals[j].sizeBytes, fp_dst);
                }
                break;
            }
        }

        // clean
        std::cout << "to clean resource" << std::endl;
        x265_encoder_close(pHander);
        x265_picture_free(pPic_in);
        x265_param_free(pParam);
        delete[] buff;
        pHander = nullptr;

        fclose(fp_src);
        fclose(fp_dst);
        return 0;
    }
    else
    {
        char *buffer;
        if ((buffer = getcwd(nullptr, 0)) == nullptr)
        {
            std::cout << "get cwd error." << std::endl;
            free(buffer);
        }
        else
        {
            std::cout << "Error open files. "
                      << "the current dir is "
                      << buffer
                      << std::endl
                      << "The fpSrc is"
                      << fp_src
                      << "the fpDst is "
                      << fp_dst
                      << std::endl;
        }
        return -1;
    }
}
```

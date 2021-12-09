# 使用X264编码视频
## 环境准备
1. 使用git clone 下载x264源码
[https://www.videolan.org/developers/x264.html](https://www.videolan.org/developers/x264.html)
2. 如果电脑之前没有安装过 yasm,nasm 可以使用 brew 安装一下
``` bash
    brew install yasm
    brew install nasm
```
3. 进入到工程根目录下编译
```bash
  // 配置开启动态库，也可以不开启，默认MAC会安装在/usr/local目录下，其他可以使用./confiure --help 查看帮助
  1.  ./configure --enable-shared 
  // 配置成功后，开启编译
  2.  make 
  // 将编译完的产物安装到指定的目录
  3.  make install
```
x264 install 成功之后命令行会输出安装到的目录
```bash
install -d /usr/local/bin
install x264 /usr/local/bin
install -d /usr/local/include /usr/local/lib/pkgconfig
install -m 644 ./x264.h x264_config.h /usr/local/include
install -m 644 x264.pc /usr/local/lib/pkgconfig
install -d /usr/local/lib
ln -f -s libx264.164.dylib /usr/local/lib/libx264.dylib
install -m 755 libx264.164.dylib /usr/local/lib
```
## 命令行使用
如
```bash
x264  -o akiyo.264 --input-res=352x288   --input-csp=i420 --input-depth=8  akiyo_cif.yuv
```
更多使用方式可以参考
```bash
x264 --help
```
## 工程使用
我使用的是vscode + cmake
因为安装的时候将pkgconfig 安装到了这个路径
/usr/local/lib/pkgconfig/x264

可以打开该文件看看内容，描写了这个库的头文件路径，二进制文件路径等
``` bash
  1 prefix=/usr/local
  2 exec_prefix=${prefix}
  3 libdir=${exec_prefix}/lib
  4 includedir=${prefix}/include
  5
  6 Name: x264
  7 Description: H.264 (MPEG4 AVC) encoder library
  8 Version: 0.164.3075
  9 Libs: -L${exec_prefix}/lib -lx264
 10 Libs.private: -lpthread -lm -ldl
 11 Cflags: -I${prefix}/include -DX264_API_IMPORTS
```
同时使用
pkg-config 命令确保,pkgconfig能够搜索到
```bash
pkgconfig pkg-config --list-all | grep x264
x264                                x264 - H.264 (MPEG4 AVC) encoder library
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
project(x264_encoder)
set(CMAKE_CXX_STANDARD 11)            # Enable c++11 standard

set(CMAKE_BUILD_TYPE Debug)
set(SOURCE_FILES X264Encoder.cpp)

# https://cmake.org/cmake/help/latest/module/FindPkgConfig.html
# https://zhuanlan.zhihu.com/p/111155574
find_package(PkgConfig REQUIRED)
// 使用PkgConfig寻找module
pkg_check_modules(x264 REQUIRED IMPORTED_TARGET x264 )

add_executable(x264_encoder ${SOURCE_FILES})

target_link_libraries(${PROJECT_NAME} PRIVATE PkgConfig::x264)
```

3. 代码
```cpp
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <iomanip>
#include <x264.h>

// https://blog.csdn.net/leixiaohua1020/article/details/42078645
int main(int argv, const char *args[])
{
    FILE *fp_src = fopen("res/akiyo_cif.yuv", "rb");
    FILE *fp_dst = fopen("res/akiyo_cif_code.h264", "wb");

    // 352x288
    int width = 352;
    int height = 288;
    int csp = X264_CSP_I420;
    // 设置为0，会根据文件大小计算帧数
    // 如果帧数设置的太小，可能会造成没有编码后的结果
    int frameNum = 0;

    if (fp_src && fp_dst)
    {
        x264_picture_t *pPic_in = new x264_picture_t();
        x264_picture_t *pPic_out = new x264_picture_t();
        x264_param_t *pParam = new x264_param_t();

        x264_param_default(pParam);

        // 根据资源的大小设置的尺寸
        // YUVView可以查看YUV资源的信息
        pParam->i_width = 352;
        pParam->i_height = 288;
        pParam->i_csp = csp;

        // https://blog.csdn.net/lcalqf/article/details/65635333
        x264_param_apply_profile(pParam, x264_profile_names[5]);

        x264_t *pHander = x264_encoder_open(pParam);

        x264_picture_init(pPic_out);
        x264_picture_alloc(pPic_in, csp, pParam->i_width, pParam->i_height);
        int ySize = pParam->i_width * pParam->i_height;
        if (frameNum == 0)
        {
            switch (csp)
            {
            case X264_CSP_I420:
                fseek(fp_src, 0, SEEK_END);
                frameNum = ftell(fp_src) / (ySize * 3 / 2);
                fseek(fp_src, 0, SEEK_SET);
                break;

            default:
                std::cout << "Colorespace are not support." << std::endl;
                return -1;
            }
        }

        x264_nal_t *pNals = nullptr;
        int iNal = 0;
        // Loop to encode
        for (int i = 0; i < frameNum; i++)
        {
            switch (csp)
            {
            case X264_CSP_I420:
                // https://www.cplusplus.com/reference/cstdio/fseek/
                fread(pPic_in->img.plane[0], ySize, 1, fp_src);     // Y
                fread(pPic_in->img.plane[1], ySize / 4, 1, fp_src); // U
                fread(pPic_in->img.plane[2], ySize / 4, 1, fp_src); // V
                break;

            default:
                std::cout << "Coloresapce are not support." << std::endl;
                return -1;
            }
            // PTS VS DTS
            // https://bitmovin.com/docs/encoding/faqs/why-is-an-timestamp-offset-for-ts-muxings-applied-by-default
            pPic_in->i_pts = i;

            int ret = x264_encoder_encode(pHander, &pNals, &iNal, pPic_in, pPic_out);
            if (ret < 0)
            {
                std::cout << "Error encode." << std::endl;
                return -1;
            }
            std::cout << "success encode frame" << std::setw(5) << i << std::endl;
            for (int j = 0; j < iNal; j++)
            {
                std::cout << "write nal to file" << std::setw(5) << j << std::endl;
                fwrite(pNals[j].p_payload, 1, pNals[j].i_payload, fp_dst);
            }
        }
        std::cout << "to flush" << std::endl;
        // flush
        while (true)
        {
            int ret = x264_encoder_encode(pHander, &pNals, &iNal, pPic_in, pPic_out);
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
                    fwrite(pNals[j].p_payload, 1, pNals[j].i_payload, fp_dst);
                }
                break;
            }
        }

        // clean
        std::cout << "to clean resource" << std::endl;
        x264_picture_clean(pPic_in);
        x264_encoder_close(pHander);
        pHander = nullptr;

        delete pPic_in;
        delete pPic_out;
        delete pParam;

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

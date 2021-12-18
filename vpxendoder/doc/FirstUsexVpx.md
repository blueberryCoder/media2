# 使用X265编码视频

## 环境准备
1. 使用git下载libvpx源码
[https://chromium.googlesource.com/webm/libvpx/](https://chromium.googlesource.com/webm/libvpx/)

2. 如果电脑之前没有安装过 yasm,nasm 可以使用 brew 安装一下
``` bash
    brew install yasm
    brew install nasm
```
3. 进入到工程根目录下编译

```bash
./configure --enable-shared
```

配置完成之后
```bash
make
make install
```

## 命令行使用
如
```bash
vpxenc --codec=vp8 -w 352 -h 288  -o akiyo1.vpx akiyo_cif.yuv
```
更多使用方式可以参考
```bash
vpxenc --help
```
## 工程使用

1. 资源准备

yuv 视频源： http://trace.eas.asu.edu/yuv/index.html
查看YUV视频源工具： https://files.cnblogs.com/littlejohnny/YUVviewer_src.rar
https://github.com/IENT/YUView/releases

我下载了一个资源名,名称为：akiyo_cif,是一个I420,8位深的资源

2. CMakeList.txt

```cmake
cmake_minimum_required(VERSION 3.14)
project(Vpx_encoder)
set(CMAKE_CXX_STANDARD 11)            # Enable c++11 standard

set(CMAKE_BUILD_TYPE Debug)
set(SOURCE_FILES VpxEncoder.cpp)

# https://cmake.org/cmake/help/latest/module/FindPkgConfig.html
# https://zhuanlan.zhihu.com/p/111155574
find_package(PkgConfig REQUIRED)
pkg_check_modules(vpx REQUIRED IMPORTED_TARGET vpx )


add_executable(Vpx_encoder ${SOURCE_FILES})

target_link_libraries(${PROJECT_NAME} PRIVATE PkgConfig::vpx)
```

3. 代码
```cpp
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <iomanip>

#include <vpx/vpx_encoder.h>
#include <vpx/vp8cx.h>
#
#define fourcc 0x30385056

#define interface (&vpx_codec_vp8_cx_algo)

#define log(msg) std::cout << msg << std::endl

static void mem_put_le16(char *mem, unsigned int val)
{
    mem[0] = val;
    mem[1] = val >> 8;
}

static void mem_put_le32(char *mem, unsigned int val)
{
    mem[0] = val;
    mem[1] = val >> 8;
    mem[2] = val >> 16;
    mem[3] = val >> 24;
}

static void write_ivf_file_header(FILE *outFile, const vpx_codec_enc_cfg_t *cfg, int frame_cnt)
{
    if (cfg->g_pass != VPX_RC_ONE_PASS && cfg->g_pass != VPX_RC_LAST_PASS)
    {
        return;
    }
    char header[32];
    header[0] = 'D';
    header[1] = 'K';
    header[2] = 'I';
    header[3] = 'F';
    mem_put_le16(header + 4, 0);                    // version
    mem_put_le16(header + 6, 32);                   // headersize;
    mem_put_le32(header + 8, fourcc);               // headersize;
    mem_put_le16(header + 12, cfg->g_w);            // width
    mem_put_le16(header + 14, cfg->g_h);            // height
    mem_put_le32(header + 16, cfg->g_timebase.den); // rate
    mem_put_le32(header + 20, cfg->g_timebase.num); // scale
    mem_put_le32(header + 24, frame_cnt);           // length;
    mem_put_le32(header + 28, 0);                   // unused;

    fwrite(header, 32, 1, outFile);
}

static void write_ivf_frame_header(FILE *outFile, const vpx_codec_cx_pkt_t *pkt)
{
    char header[12];
    vpx_codec_pts_t pts;
    if (pkt->kind != VPX_CODEC_CX_FRAME_PKT)
    {
        return;
    }
    pts = pkt->data.frame.pts;
    mem_put_le32(header, pkt->data.frame.sz);
    mem_put_le32(header + 4, pts & 0xFFFFFFFF);
    mem_put_le32(header + 8, pts >> 32);

    fwrite(header, 1, 12, outFile);
}

// https://blog.csdn.net/leixiaohua1020/article/details/42079101
// I420: https://blog.csdn.net/leixiaohua1020/article/details/12234821
int main(int argv, const char *args[])
{
    FILE *fp_src = fopen("../res/akiyo_cif.yuv", "rb");
    FILE *fp_dst = fopen("./akiyo_cif_code.vp8", "wb");

    // 352x288
    int width = 352;
    int height = 288;
    int csp = VPX_IMG_FMT_I420;
    // 设置为0，会根据文件大小计算帧数
    // 如果帧数设置的太小，可能会造成没有编码后的结果
    // int frameNum = 0;
    int frame_cnt = 0;
    int flags = 0;

    if (fp_src && fp_dst)
    {
        vpx_image_t raw;
        if (!vpx_img_alloc(&raw, VPX_IMG_FMT_I420, width, height, 1))
        {
            log("Error open files");
            return -1;
        }
        std::cout << "Using: " << vpx_codec_iface_name(interface) << std::endl;
        vpx_codec_enc_cfg_t cfg;
        int ret = vpx_codec_enc_config_default(interface, &cfg, 0);
        if (ret != VPX_CODEC_OK)
        {
            log("Error config ");
            return -1;
        }
        cfg.rc_target_bitrate = 800;
        cfg.g_w = width;
        cfg.g_h = height;

        write_ivf_file_header(fp_dst, &cfg, 0); // frame count 0 ?
        vpx_codec_ctx_t codec;
        // Initialize codec .
        if (vpx_codec_enc_init(&codec, interface, &cfg, 0) != VPX_CODEC_OK)
        {
            log("Failed to initialize encoder");
        }
        int frame_avail = 1;
        int got_data = 0;
        int y_size = width * height;
        while (frame_avail || got_data)
        {
            vpx_codec_iter_t iter = nullptr;
            if (fread(raw.planes[0], 1, y_size * 3 / 2, fp_src) != y_size * 3 / 2)
            {
                frame_avail = 0;
            }
            if (frame_avail)
            {
                ret = vpx_codec_encode(&codec, &raw, frame_cnt, 1, flags, VPX_DL_REALTIME);
            }
            else
            {
                ret = vpx_codec_encode(&codec, nullptr, frame_cnt, 1, flags, VPX_DL_REALTIME);
            }

            if (ret)
            {
                log("Failed to  encode frame.");
                return -1;
            }
            got_data = 0;
            const vpx_codec_cx_pkt_t *pkt;
            while ((pkt = vpx_codec_get_cx_data(&codec, &iter)))
            {
                got_data = 1;
                switch (pkt->kind)
                {
                case VPX_CODEC_CX_FRAME_PKT:
                    write_ivf_frame_header(fp_dst, pkt);
                    fwrite(pkt->data.frame.buf, 1, pkt->data.frame.sz, fp_dst);
                    break;

                default:
                    break;
                }
            }
            std::cout << "Succeed encode frame: " << frame_cnt << std::endl;
            frame_cnt++;
        }
        vpx_img_free(&raw);
        fclose(fp_src);
        vpx_codec_destroy(&codec);
        if (!fseek(fp_dst, 0, SEEK_SET))
        {
            write_ivf_file_header(fp_dst, &cfg, frame_cnt - 1);
        }
        fclose(fp_dst);
        return 0;
    }
    else
    {
        char *buffer;
        if ((buffer = getcwd(nullptr, 0)) == nullptr)
        {
            log("get cwd error.");
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

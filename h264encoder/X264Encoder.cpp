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
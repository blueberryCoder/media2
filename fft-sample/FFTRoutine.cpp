#include <iostream>
#include <memory>
#include <string>
#include <array>
#include <iomanip>
extern "C"
{
#include "Mayer_FFT.c"

    // extern void mayer_realfft(int n, REAL *real);
}
class FFTRoutine
{

public:
    FFTRoutine(int nfft) : nfft_(nfft)
    {
    }

    ~FFTRoutine() {}

    /**
     * @brief  将时域信号转换为频域信号。
     *
     * @param input 时域信号（浮点类型表示）
     * @param output_re 转换为频域信号的实部。
     * @param output_im 转换为频域信号的的虚部。
     */
    void fft_forward(float *input, float *output_re, float *output_im)
    {
        source_ptr_.reset(new float[nfft_]());
        memcpy(source_ptr_.get(), input, nfft_ * sizeof(float));
        mayer_realfft(nfft_, source_ptr_.get());

        output_im[0] = 0;
        for (int i = 0; i < nfft_ / 2; i++)
        {
            output_re[i] = source_ptr_[i];
            output_im[i + 1] = source_ptr_[nfft_ - 1 - i];
        }

        source_ptr_.release();
    }

    /**
     * @brief 将频域信号转换为时域信号。
     *
     * @param input_re 输入的时部
     * @param input_im 输入的虚部
     * @param output 输出
     */
    void fft_inverse(float *input_re, float *input_im, float *output)
    {
        source_ptr_.reset(new float[nfft_]());
        int hnfft = nfft_ / 2;
        for (int ti = 0; ti < hnfft; ti++)
        {
            source_ptr_[ti] = input_re[ti];
            source_ptr_[nfft_ - 1 - ti] = input_im[ti + 1];
        }
        output[hnfft] = input_re[hnfft];
        memcpy(output, source_ptr_.get(), nfft_ * sizeof(float));
        source_ptr_.release();
    }

private:
    std::unique_ptr<float[]> source_ptr_;
    int nfft_;
};

int main(int argc, const char *argv[])
{
    // https://rosettacode.org/wiki/Fast_Fourier_transform#C.2B.2B
    using namespace std;
    FFTRoutine routine(8);

    std::array<float, 8> tarray{1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0};
    std::array<float, 4> frearray{};
    std::array<float, 5> fimarray{};

    routine.fft_forward(tarray.data(), frearray.data(), fimarray.data());

    std::cout << "input:" << std::endl;
    for (auto &x : tarray)
    {
        std::cout << x << ",";
    }
    std::cout << std::endl;
    std::cout << "output" << std::endl;
    for (auto &x : frearray)
    {
        std::cout << setw(10) << setiosflags(ios::left) << x ;
    }
    std::cout << std::endl;
    for (auto &x : fimarray)
    {
        std::cout << setw(10) << setiosflags(ios::left) << x ;
    }
    std::cout << std::endl;

    std::array<float, 8> output{};
    routine.fft_inverse(frearray.data(), fimarray.data(), output.data());

    std::cout  << std::endl << "output:" << std::endl;
    for (auto &x : output)
    {
        std::cout << setw(10) << x ;
    }
    std::cout << std::endl;
    return 0;
};
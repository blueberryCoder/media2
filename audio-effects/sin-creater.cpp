#include <iostream>
#include <fstream>
#include <math.h>

// create a sin pcm
// frequence 440HZ
// sample rate 44100
// short
// duration 5s
// one chanel
int main(int argc, const char *argv[])
{
    const int sample_rate = 44100;
    const int duration = 5;
    const int frequence = 440;

    const int sample_count = sample_rate * duration;
    short samples[sample_count];

    double tancr = frequence * 2 * M_PI * 1.0 / sample_rate;
    short *ptr = samples;
    double angle = 0.0;
    std::cout << "trancr " << tancr << std::endl;
    for (int i = 0; i < sample_count; i++)
    {
        std::cout << angle << std::endl;
        double amptitude = sin(angle);
        // double amptitude = (sin(angle) + sin(angle * 2 + M_PI / 3) + sin(angle * 3 + M_PI / 2) + sin(angle * 4 + M_PI / 4)) * 0.25;
        *ptr = amptitude * SHRT_MAX;
        ptr++;
        angle += tancr;
    }

    // write file
    std::ofstream output_file("./sin.pcm", std::ofstream::out);
    if (output_file)
    {
        output_file.write(reinterpret_cast<const char *>(samples), sample_count * sizeof(short));
    }
}
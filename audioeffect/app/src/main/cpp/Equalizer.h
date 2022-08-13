//
// Created by bytedance on 2022/7/16.
//

#ifndef AUDIOEFFECT_EQUALIZER_H
#define AUDIOEFFECT_EQUALIZER_H

#include <string>

#include "sox.h"

using namespace std;

class Equalizer {

public:
    Equalizer();

    virtual ~Equalizer();

    void setInput(string input);

    void setOutput(string output);

    void start();

private:
    string input_;
    string output_;
    sox_format_t *in_;
    sox_format_t *out_;
    sox_effects_chain_t * chain_;
};


#endif //AUDIOEFFECT_EQUALIZER_H

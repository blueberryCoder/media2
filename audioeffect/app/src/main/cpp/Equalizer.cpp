//
// Created by bytedance on 2022/7/16.
//

#include "Equalizer.h"
#include "logger.h"

#include <utility>
#include <fstream>

Equalizer::Equalizer() {
}

Equalizer::~Equalizer() {}

void Equalizer::setInput(string input) {
    logi("%s,%s", "setInput", input.c_str());
    input_ = std::move(input);
}

void Equalizer::setOutput(string output) {
    logi("%s,%s", "setOutput", output.c_str());
    output_ = std::move(output);
}

void Equalizer::start() {
    assert(sox_init() == SOX_SUCCESS);

    in_ = sox_open_read(input_.c_str(), nullptr, nullptr, nullptr);
    out_ = sox_open_write(output_.c_str(), &in_->signal, nullptr, nullptr, nullptr, nullptr);
    chain_ = sox_create_effects_chain(&in_->encoding, &out_->encoding);

    // 输入
    sox_effect_t *e = sox_create_effect(sox_find_effect("input"));
    char *args[10];
    args[0] = (char *) in_;
    logi("in paths is %s:", args[0]);

    int result = sox_effect_options(e, 1, args);
    logi("effect input result is %d", result);
    assert(result == SOX_SUCCESS);
    sox_add_effect(chain_, e, &in_->signal, &in_->signal);
    free(e);

    // 均衡器
    e = sox_create_effect(sox_find_effect("equalizer"));
    char *frequency = "300";
    char *bandwidth = "1.25q";
    char *gain = "3";
    char *equalizer_chars[] = {frequency, bandwidth, gain};
    result = sox_effect_options(e, 3, equalizer_chars);
    if (result != SOX_SUCCESS) {
        logi("option equalizer failed : error code is %d", result);
        assert(result == SOX_SUCCESS);
    }
    sox_add_effect(chain_, e, &in_->signal, &out_->signal);
    free(e);

    e = sox_create_effect(sox_find_effect("equalizer"));
    frequency = "300";
    bandwidth = "1.25q";
    gain = "30";
    equalizer_chars[0] = frequency;
    equalizer_chars[1] = bandwidth;
    equalizer_chars[2] = gain;
    result = sox_effect_options(e, 3, equalizer_chars);

    if (result != SOX_SUCCESS) {
        logi("option equalizer failed : error code is %d", result);
        assert(result == SOX_SUCCESS);
    }
    sox_add_effect(chain_, e, &in_->signal, &out_->signal);
    free(e);

    // 输出
    e = sox_create_effect(sox_find_effect("output"));
    args[0] = (char *) out_;
    result = sox_effect_options(e, 1, args);
    if (result != SOX_SUCCESS) {
        logi("option output effect failed : error code is %d", result);
        assert(result == SOX_SUCCESS);
    }
    sox_add_effect(chain_, e, &out_->signal, &out_->signal);
    free(e);

    sox_flow_effects(chain_, nullptr, nullptr);
    sox_delete_effects_chain(chain_);
    sox_close(out_);
    sox_close(in_);
    sox_quit();
}
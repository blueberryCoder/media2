# 使用MediaCodec+OpenSL编写简单的音频播放器
## 前言
通过MediaCodec Native API 和OpenSL编写一个简单的音频播放器。可以解码并播放一个mp3文件.
## 流程
### 初始化
1. 使用AMediaExtractor解析Mp3文件，它可以得到音频文件的格式、以及帧（未解码）。
2. 根据得到的音频信息（channelCount,channelMask,sampleRate)等可以创建出OpenSL AudioPlayer来播放解码后的音频。
3. 创建根据解析得到的音频mime,创建合适的MediaCodec编码器
### 解码放入消费队列
1. 使用`AMediaCodec_start`开始解码。
2. 使用`AMediaCodec_dequeueInputBuffer`获取到一个空的buffer.
3. 使用`AMediaExtractor_readSampleData`获取到未解码的数据放入到buffer,读完之后需要使用`AMediaExtractor_advance`讲指针前移，这样下一次再读的时候就是下一个帧数据了。
4. 使用`AMediaCodec_queueInputBuffer`获取一个输入buffer的index,使用index通过`AMediaCodec_getInputBuffer`获取输入buffer。
5. 使用`AMediaCodec_dequeueOutputBuffer`获取一个输出buffer的index,使用index通过`AMediaCodec_getOutputBuffer`获取对应的输出buffer。
6. 将输出buffer的内容放入到一个消费队列，带openSL播放器消费。
### 读取消费队列传给openSL
1. 将openSL的AudioPlayer状态设置为开始，并传入一个空的buffer.opensl播放器会进行回调来获取数据。
2. 在会掉种读取消费队列的数据。 
## 代码片段
### 初始化
```cpp 
JNIEXPORT jlong JNICALL
Java_com_blueberry_videoplayer_SLMediaCodecAudio_initialize(JNIEnv *env, jobject thiz,
                                                            jobject assetManager,
                                                            jstring res_path) {

    auto engine = new AudioPlayerEngine();
    env->GetJavaVM(&engine->javaVm);
    // 创建消费队列
    engine->buffer_queue_ = new SafeQueue<MyBuffer>();
    const char *fileName = env->GetStringUTFChars(res_path, NULL);
    auto cAssetManager = AAssetManager_fromJava(env, assetManager);
    engine->asset_ = AAssetManager_open(cAssetManager, fileName, AASSET_MODE_RANDOM);
    off_t start = 0, len = 0;
    auto fd = AAsset_openFileDescriptor(engine->asset_, &start, &len);
    engine->mediaExtractor_ = AMediaExtractor_new();
    AMediaExtractor_setDataSourceFd(engine->mediaExtractor_, fd, start, len);
    AMediaExtractor_getTrackCount(engine->mediaExtractor_);
    auto format = AMediaExtractor_getTrackFormat(engine->mediaExtractor_, 0);
    const char *mime = nullptr;
    AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, reinterpret_cast<const char **>(&mime));
    AMediaFormat_getInt64(format, AMEDIAFORMAT_KEY_DURATION, &engine->metaData_.duration_);
    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &engine->metaData_.channelCount_);
    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_SAMPLE_RATE, &engine->metaData_.sampleRate_);
    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_BIT_RATE, &engine->metaData_.bitrate_);

    const char *formatStr = AMediaFormat_toString(format);

    LOGD("the file format is %s", formatStr);
    AMediaExtractor_selectTrack(engine->mediaExtractor_, 0);

    // 创建解码器，并配置
    engine->codec_ = AMediaCodec_createDecoderByType(mime);
    AMediaCodec_configure(engine->codec_, format, nullptr, nullptr, 0);

    // 创建openSL引擎
    SLresult result = slCreateEngine(&engine->slEngineObj_, 0, nullptr, 0, nullptr, nullptr);
    result = (*engine->slEngineObj_)->Realize(engine->slEngineObj_, false);
    result = (*engine->slEngineObj_)->GetInterface(engine->slEngineObj_, SL_IID_ENGINE,
                                                   &engine->slEngineItf_);

    // 根据音频格式信息，构建音频播放器的输入格式
    SLAndroidDataFormat_PCM_EX_ sourceFormat{
            .formatType = SL_DATAFORMAT_PCM,
            .numChannels = static_cast<SLuint32>(engine->metaData_.channelCount_),
            // millherzt
            .sampleRate = static_cast<SLuint32>(engine->metaData_.sampleRate_) * 1000,
            .bitsPerSample =SL_PCMSAMPLEFORMAT_FIXED_16,
            .containerSize = SL_PCMSAMPLEFORMAT_FIXED_16,
            .channelMask =  SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
            .endianness = SL_BYTEORDER_LITTLEENDIAN,
            .representation = SL_ANDROID_PCM_REPRESENTATION_SIGNED_INT
    };

    engine->silent_buf_ = new uint8_t[2 * 2 * 80]; // 80 frame per buf.
    engine->silent_buf_size_ = 2 * 2 * 80;
    // 输入数据使用内存队列
    SLDataLocator_AndroidSimpleBufferQueue androidSimpleBufferQueueLocator{
            .locatorType = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            .numBuffers = 4
    };

    SLDataSource slDataSource{
            .pLocator = &androidSimpleBufferQueueLocator,
            .pFormat = &sourceFormat,
    };

    SLObjectItf outputMixObj;
    result = (*engine->slEngineItf_)->CreateOutputMix(engine->slEngineItf_,
                                                      &outputMixObj,
                                                      0,
                                                      nullptr,
                                                      nullptr);
    result = (*outputMixObj)->Realize(outputMixObj, false);
    SLDataLocator_OutputMix outputMixLocator{
            .locatorType = SL_DATALOCATOR_OUTPUTMIX,
            .outputMix = outputMixObj,
    };
    SLDataSink slDataSink{
            .pLocator = &outputMixLocator,
            .pFormat =  nullptr,
    };

    SLInterfaceID interfaces[2]{
            SL_IID_VOLUME,
            SL_IID_BUFFERQUEUE
    };
    SLboolean isRequired[2]{
            SL_BOOLEAN_TRUE,
            SL_BOOLEAN_TRUE
    };
    (*engine->slEngineItf_)->CreateAudioPlayer(engine->slEngineItf_,
                                               &engine->slPlayerObj_,
                                               &slDataSource, &slDataSink,
                                               sizeof(interfaces) /
                                               sizeof(interfaces[0]),
                                               interfaces, isRequired);

    (*engine->slPlayerObj_)->Realize(engine->slPlayerObj_, SL_BOOLEAN_FALSE);

    (*engine->slPlayerObj_)->GetInterface(engine->slPlayerObj_, SL_IID_PLAY,
                                          &engine->slPlayItf_);

    (*engine->slPlayerObj_)->GetInterface(engine->slPlayerObj_,
                                                   SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                                   &engine->androidSimpleBufferQueueItf_);
 
    result = (*engine->androidSimpleBufferQueueItf_)->RegisterCallback(
            engine->androidSimpleBufferQueueItf_, bufferQueueCallback, engine);

    assert(result == SL_RESULT_SUCCESS);
    result = (*engine->slPlayItf_)->SetPlayState(engine->slPlayItf_, SL_PLAYSTATE_STOPPED);

    // 创建解码线程
    pthread_create(&engine->thread_, nullptr, threadRun, engine);
    env->ReleaseStringUTFChars(res_path, fileName);
    return reinterpret_cast<long>(engine);
}
```
### 解码
```cpp
if (engine->status_ == Status::STARTED) {
            // 开始解码
            AMediaCodec_start(engine->codec_);
            while (engine->status_ == STARTED) {
                if (!engine->inputEnd) {
                    // 读区一个输入buffer
                    auto index = AMediaCodec_dequeueInputBuffer(engine->codec_, TIMEOUT_US);
                    if (index >= 0) {
                        size_t output_size = 0;
                        auto buffer = AMediaCodec_getInputBuffer(engine->codec_, index,
                                                                 &output_size);
                        // 将为解码输入放入buffer                                         
                        auto sampleSize = AMediaExtractor_readSampleData(engine->mediaExtractor_,
                                                                         buffer,
                                                                         output_size);
                        if (sampleSize <= 0) {
                            sampleSize = 0;
                            engine->inputEnd = true;
                        }
                        auto presentationTimeUs = AMediaExtractor_getSampleTime(
                                engine->mediaExtractor_);
                        // 将buffer放会解码器
                        auto status = AMediaCodec_queueInputBuffer(engine->codec_,
                                                                   index,
                                                                   0,
                                                                   sampleSize,
                                                                   presentationTimeUs,
                                                                   engine->inputEnd
                                                                   ? AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM
                                                                   : 0);

                        if (status != AMEDIA_OK) {
                            LOGD("status:%d", status);
                            break;
                        }
                        // 前移，以便下次AMediaExtractor_readSampleData将获取到新的数据
                        AMediaExtractor_advance(engine->mediaExtractor_);
                    }
                }

                // decode
                while (!engine->outputEnd) {
                    AMediaCodecBufferInfo bufferInfo;
                    // 获取一个输出buffer index
                    auto index = AMediaCodec_dequeueOutputBuffer(engine->codec_, &bufferInfo,
                                                                 TIMEOUT_US);
                    if (index >= 0) {
                        LOGD("process output index >=0,%d", index);
                        if (bufferInfo.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                            engine->outputEnd = true;
                        }
                        size_t buffer_size = 0;
                        // 获取输出的buffer
                        auto buffer = AMediaCodec_getOutputBuffer(engine->codec_, index,
                                                                  &buffer_size);

                        if (bufferInfo.size > 0) {
                            auto buf = new uint8_t[bufferInfo.size];
                            memcpy(buf, &buffer[bufferInfo.offset], bufferInfo.size);
                            MyBuffer myBuffer;
                            myBuffer.len_ = bufferInfo.size;
                            myBuffer.buf_ = buf;
                            // 将解码后的数据放入到消费队列
                            engine->buffer_queue_->enqueue(myBuffer);
                        }
                        // 释放输出buffer
                        AMediaCodec_releaseOutputBuffer(engine->codec_, index, false);
                        LOGD("bufferQueueCallback:index >=0 ");
                        break;
                    } else {
                        switch (index) {
                            case AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED  : {
                                auto format = AMediaCodec_getOutputFormat(engine->codec_);
                                const char *strFormat = AMediaFormat_toString(format);
                                const char *pcmEncoding = nullptr;
                                AMediaFormat_delete(format);
                                break;
                            }
                            case AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED: {
                                break;
                            }
                            case AMEDIACODEC_INFO_TRY_AGAIN_LATER: {
                            }
                            default: {
                                break;
                            }
                        }
                    }
                } 
            }
```
### openSL消费队列
```cpp
void bufferQueueCallback(SLAndroidSimpleBufferQueueItf androidSimpleBufferQueueItf, void *context) {
    auto engine = reinterpret_cast<AudioPlayerEngine * >(context);

    auto queue = engine->buffer_queue_;
    auto myBuffer = queue->dequeue();
    if (myBuffer.buf_ != nullptr) {
        // 获取到消费队列中解码后的数据，放入opensl的内存队列中
        (*engine->androidSimpleBufferQueueItf_)->Enqueue(
                engine->androidSimpleBufferQueueItf_,
                myBuffer.buf_,
                myBuffer.len_);
    } else {
        (*engine->androidSimpleBufferQueueItf_)->Enqueue(
                engine->androidSimpleBufferQueueItf_,
                engine->silent_buf_,
                1);
    }
    return;
}
```

## 参考
https://developer.android.com/ndk/reference/group/media
https://developer.android.com/reference/android/media/MediaCodec#queueInputBuffer(int,%20int,%20int,%20long,%20int)

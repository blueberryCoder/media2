# 解读OpenSL回声Demo
## 前言
回声App是google的一个使用openSL的示例。
代码地址:https://github.com/android/ndk-samples/tree/main/audio-echo
通过代码可以学习到如何简单使用OpenSL采集音频，播放音频。
## app的设计图
[](OpenSLEcho.png)

## 流程
App的大致流程为：
1. 使用slCreateEngine创建出一个SLObjectItf然后通过调用它的GetInterace方法获取一个SLEngineItf。
```c++
// 1. 创建slEngineObj
result = slCreateEngine(&engine.slEngineObj_, 0, NULL, 0, NULL, NULL);
SLASSERT(result);
// 2. realize slEngineObj
result = (*engine.slEngineObj_)->Realize(engine.slEngineObj_, SL_BOOLEAN_FALSE);
SLASSERT(result);
// 3. 获取 SLEngineItf
result = (*engine.slEngineObj_)->GetInterface(engine.slEngineObj_, SL_IID_ENGINE, &engine.slEngineItf_);
SLASSERT(result);
```
2. 初始化2个队列recQueue和freeQueue。RecQueue中的用来存储录制的数据并提供数据给播放。FreeQueue的buf为AudioPlayer使用完的buffer。
            refQueue
     ----载有数据的buffer-------->    
录制                              播放
     <--------空buff-------------   
          freeQueue  
3. 使用SLEngineItf创建AudioPlayer,创建AudioPlayer的时候需要指定输入以及输出。对于AudioPlayer来讲，输出为Outputmix
输入为内存队列
- 创建SLDataLocator_AndroidSimpleBufferQueue作为AudioPlayer的输入.
- 创建SLDataLocator_OutputMix作为AudioPlayer的输出。
- 以上的输入输出类型需要使用SLDataSource/SLDataSink进行包装然后在创建AudioPlayer的时候传入
- 设置callback: slAndroidSimpleBufferQueueCallback 应用程序将在callback中为SLDataLocator_AndroidSimpleBufferQueue提供数据

```c++
sult = (*slEngine)->CreateOutputMix(slEngine, &outputMixObjectItf_, 0, NULL, NULL);
SLASSERT(result);
result = (*outputMixObjectItf_)->Realize(outputMixObjectItf_, SL_BOOLEAN_FALSE);
SLASSERT(result);

// 输入使用BufferQueue
SLDataLocator_AndroidSimpleBufferQueue locBufQ = {
        SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, DEVICE_SHADOW_BUFFER_QUEUE_LEN};
SLAndroidDataFormat_PCM_EX_ format_pcm;
ConvertToSLSampleFormat(&format_pcm, &sampleInfo_);
SLDataSource audioSrc = {&locBufQ, &format_pcm};
// 输出到OutputMix
SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObjectItf_};
SLDataSink audioSink = {&loc_outmix, NULL};
SLInterfaceID ids[2] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
result = (*slEngine)->CreateAudioPlayer(slEngine, &playerObjectItf_, &audioSrc,
                                        &audioSink, sizeof(ids) / sizeof(ids[0]), ids, req);
SLASSERT(result);
// realize the player
result = (*playerObjectItf_)->Realize(playerObjectItf_, SL_BOOLEAN_FALSE);
SLASSERT(result);
result = (*playerObjectItf_)->GetInterface(playerObjectItf_, SL_IID_PLAY,
                                           &playItf_);
SLASSERT(result);
result = (*playerObjectItf_)->GetInterface(playerObjectItf_, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &playBufferQueueItf_);
SLASSERT(result);
result = (*playBufferQueueItf_)->RegisterCallback(playBufferQueueItf_, bqPlayerCallback, this);
```
4. 使用SLengineItf创建AudioRecorder,同创建AudioPlayer一样需要指定输入和输出
- 创建SLDataLocator_IODevice作为AudioRecorder的输入。
- 创建SLDataLocator_AndroidSimpleBufferQueue作为AudioRecorder的输出。
- 同样需要使用SLDataSource/SLDataSink对上面的输入和输出进行包装。
- 为SLDataLocator_AndroidSimpleBufferQueue设置callback,在callback中进行读取录制好的数据以及提供空buffer给slAndroidSimpleBufferQueueCallback。
```c++
// 输入
SLDataLocator_IODevice loc_dev = {
        SL_DATALOCATOR_IODEVICE,
        SL_IODEVICE_AUDIOINPUT,
        SL_DEFAULTDEVICEID_AUDIOINPUT, NULL};
SLDataSource audioSrc = {&loc_dev, nullptr};
// 输出
SLDataLocator_AndroidSimpleBufferQueue loc_bq = {
        SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, DEVICE_SHADOW_BUFFER_QUEUE_LEN};
SLDataSink audioSink = {&loc_bq, &format_pcm};
const SLInterfaceID id[2] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_ANDROIDCONFIGURATION};
const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
result = (*engineEngine)->CreateAudioRecorder(engineEngine, &recObjectItf_, &audioSrc, &audioSink, sizeof(id) / sizeof(id[0]), id, req);
SLASSERT(result);
SLAndroidConfigurationItf inputConfig;
result = (*recObjectItf_)->GetInterface(recObjectItf_, SL_IID_ANDROIDCONFIGURATION,
                                        &inputConfig);
if (SL_RESULT_SUCCESS == result) {
    SLuint32 presetValue = SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION;
    (*inputConfig)->SetConfiguration(inputConfig, SL_ANDROID_KEY_RECORDING_PRESET, &presetValue,
                                     sizeof(SLuint32));
}
result = (*recObjectItf_)->Realize(recObjectItf_, SL_BOOLEAN_FALSE);
SLASSERT(result);
result = (*recObjectItf_)->GetInterface(recObjectItf_, SL_IID_RECORD, &recItf_);
SLASSERT(result);
result = (*recObjectItf_)->GetInterface(recObjectItf_, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_BUFFERQUEUE, &recBufQueueItf_);
SLASSERT(result);
result = (*recBufQueueItf_)->RegisterCallback(recBufQueueItf_, bqRecorderCallback, this);
SLASSERT(result);
devShadowQueue_ = new AudioQueue(DEVICE_SHADOW_BUFFER_QUEUE_LEN);
assert(devShadowQueue_);
```
5. 创建一个AudioDelay类，用户处理录制好的数据。在收到录制好的数据后使用这个类根据设置的参数进行处理数据。

```c++
// 录制SLAndroidSimpleBufferQueueItf的callback中
void AudioRecorder::ProcessSLCallback(SLAndroidSimpleBufferQueueItf bq) {
    assert(bq == recBufQueueItf_);
    sample_buf *dataBuf = nullptr;
    devShadowQueue_->front(&dataBuf);
    devShadowQueue_->pop();
    dataBuf->size_ = dataBuf->cap_;

    // callback中会处理录制的pcm数据
    callback_(ctx_, ENGINE_SERVICE_MSG_RECORDED_AUDIO_AVAILABLE, dataBuf);
    recQueue_->push(dataBuf);
    sample_buf *freeBuf;
    while (freeQueue_->front(&freeBuf) && devShadowQueue_->push(freeBuf)) {
        freeQueue_->pop();
        SLresult result = (*bq)->Enqueue(bq, freeBuf->buf_, freeBuf->cap_);
        SLASSERT(result);
    }
    ++audioBufCount;
    if (devShadowQueue_->size() == 0) {
        (*recItf_)->SetRecordState(recItf_, SL_RECORDSTATE_STOPPED);
    }
}
```
6. 开始录制/播放， 向SLDataLocator_AndroidSimpleBufferQueue作为AudioPlayer的输入Enqueue一个buf,
并使用SetPlayState将AudioPlayer的状态设置问题PLAYING,将AudioRecorder的状态设置RECORDING,并向它对应的SLDataLocator_AndroidSimpleBufferQueue
塞入空buffer.
```c++
SLresult AudioPlayer::Start() {
    SLuint32 state;
    SLresult result = (*playItf_)->GetPlayState(playItf_, &state);
    if (result != SL_RESULT_SUCCESS) {
        return SL_BOOLEAN_FALSE;
    }
    if (state == SL_PLAYSTATE_PLAYING) {
        return SL_BOOLEAN_TRUE;
    }
    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_STOPPED);
    SLASSERT(result);

    // 插入buffer
    result = (*playBufferQueueItf_)->Enqueue(playBufferQueueItf_, silentBuf_.buf_,
                                             silentBuf_.size_);
    SLASSERT(result);
    devShadowQueue_->push(&silentBuf_);
    result = (*playItf_)->SetPlayState(playItf_, SL_PLAYSTATE_PLAYING);
    SLASSERT(result);
    return SL_BOOLEAN_TRUE;
}
SLboolean AudioRecorder::Start() {
    if (!freeQueue_ || !recQueue_ || !devShadowQueue_) {
        return SL_BOOLEAN_FALSE;
    }
    audioBufCount = 0;
    SLresult result;
    result = (*recItf_)->SetRecordState(recItf_, SL_RECORDSTATE_STOPPED);
    SLASSERT(result);
    result = (*recBufQueueItf_)->Clear(recBufQueueItf_);
    SLASSERT(result);
    for (int i = 0; i < RECORD_DEVICE_KICKSTART_BUF_COUNT; i++) {
        sample_buf *buf = nullptr;
        if (!freeQueue_->front(&buf)) {
            LOGE("====OutOfFreeBuffers @ startingRecording @ (%d)", i);
            break;
        }
        freeQueue_->pop();
        assert(buf->buf_ && buf->cap_ && !buf->size_);
        result = (*recBufQueueItf_)->Enqueue(recBufQueueItf_, buf->buf_, buf->cap_);
        SLASSERT(result);
        devShadowQueue_->push(buf);
    }
    result = (*recItf_)->SetRecordState(recItf_, SL_RECORDSTATE_RECORDING);
    SLASSERT(result);
    return (result == SL_RESULT_SUCCESS ? SL_BOOLEAN_TRUE : SL_BOOLEAN_FALSE);
}

```
7. 停止录制/播放,将AudioPlayer的状态改为STOPPED,将AudioRecorder的状态改为STOPPED，并清理队列
8. 销毁，释放queue,调用(*engine.slEngineObj_)->Destroy(engine.slEngineObj_);销毁

## 原理
1. 如何实现的decay?
decay是根据传入的decay参数（decay 介于0.0～1.0之间）并有一段buffer,将buffer之前的数据乘以decay并加上当前的数据乘以（1-decay).
比如decay为0.1， buffer是100
PCM的数为:
[1000,1000] .........[2000,2000],
pos= i               pos = i+100
最终的计算结果为[1900,1900]; 1000 * 0.1 + 2000 * 0.9. 计算好的结果又会写入到buffer中

代码：
```c++
int32_t sampleCount = channelCount_ * numFrames;
int16_t *samples = &static_cast<int16_t *>(buffer_)[curPos_ * channelCount_];
for (size_t idx = 0; idx < sampleCount; idx++) {
    // feedBackFactor 相当于decay,liveAudioFactor相当于1-decay
    // sample是之前的数据，liveAudio是当前的数据
    int32_t curSample = (samples[idx] * feedbackFactor_ + liveAudio[idx] * liveAudioFactor_) /
                        kFloatToIntMapFactor;
    if (curSample > SHRT_MAX) {
        curSample = SHRT_MAX;
    } else if (curSample < SHRT_MIN) {
        curSample = SHRT_MIN;
    }
    liveAudio[idx] = samples[idx];
    samples[idx] = static_cast<int16_t >(curSample);
}
```
2. 如何实现的delay?
因为在为AudioRecorder创建SLDataSink以及为AudioPlayer创建SLDataSource的时候设置了format.
根据应用输入的delay以及我们传入的采样率sampleRate可以计算出我们delay多少帧,根据我们设置的pcm格式可以算出来这些帧需要多少内存buffer.
将内存的大小设置为它。就可以实现delay
代码：
```c++
// delayTime 决定分配缓存的大小。
void AudioDelay::allocateBuffer() {
    float floatDelayTime = (float) delayTime_ / kMsPerSec;
    // num of frame in per second.
    float fNumFrames = floatDelayTime * (sampleRate_ / kMsPerSec);
    size_t sampleCount = static_cast<uint32_t >(fNumFrames + 0.5f) * channelCount_;
    uint32_t bytePerSample = format_ / 8;
    assert(bytePerSample <= 4 && bytePerSample);
    uint32_t bytePerFrame = channelCount_ * bytePerSample;
    bufCapacity_ = sampleCount * bytePerSample;
    bufCapacity_ = ((bufCapacity_ + bytePerFrame - 1) / bytePerFrame) * bytePerFrame;
    buffer_ = new uint8_t[bufCapacity_];
    assert(buffer_);
    memset(buffer_, 0, bufCapacity_);
    curPos_ = 0;
    bufSize_ = bufCapacity_ / bytePerSample;
}
```
## 总结
1. OpenSL中的API操作对象的一般为:首先构造出一个object对象，再Realize一下。然后使用这个对象的GetInterface并传入一个id
获取到一个xxxItf对象，通过这个Itf作些操作。
如:SLEngine,首先使用slCreateEngine获取了slEngineObj，然后需要调用 (*engine.slEngineObj_)->Realize(engine.slEngineObj_, SL_BOOLEAN_FALSE)
进行初始化，在获取slEngineItf,(*engine.slEngineObj_)->GetInterface(engine.slEngineObj_, SL_IID_ENGINE, &engine.slEngineItf_);
AudioPlayer，AudioRecorder也使用了同样的方式。
2. openSL函数的更过资料可以参考：https://www.khronos.org/registry/OpenSL-ES/specs/OpenSL_ES_Specification_1.1.pdf
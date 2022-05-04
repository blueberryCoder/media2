//
// Created by bytedance on 2022/5/2.
//

#ifndef MEDIAPLAYER_AVSYNC_H
#define MEDIAPLAYER_AVSYNC_H

#include "safe_queue.h"
#include "AudioFrame.h"
#include "VideoFrame.h"
#include <condition_variable>
#include <mutex>

class AVSync {

public:

    AVSync();
    virtual ~AVSync();

    void enqueueAudioFrame(const AudioFrame &audioFrame);

    void enqueueVideoFrame(const VideoFrame &videoFrame);

    AudioFrame dequeueAudioFrame();

    VideoFrame dequeueVideoFrame();

private:
    SafeQueue<VideoFrame> video_queue_;
    SafeQueue<AudioFrame> audio_queue_;
    std::condition_variable cv_;
    mutable std::mutex m_;
    int64_t  last_dequeue_pts_ = 0;

};


#endif //MEDIAPLAYER_AVSYNC_H

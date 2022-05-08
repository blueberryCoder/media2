//
// Created by bytedance on 2022/5/2.
//

#include "AVSync.h"

#include "logger.h"
#include <thread>
#include <chrono>

AVSync::AVSync() : video_queue_(), audio_queue_(), cv_(), m_() {
}

AVSync::~AVSync() {
}
void AVSync::enqueueAudioFrame(const AudioFrame &audioFrame) {
    audio_queue_.enqueue(audioFrame);
}

void AVSync::enqueueVideoFrame(const VideoFrame &videoFrame) {
    video_queue_.enqueue(videoFrame);
}

AudioFrame AVSync::dequeueAudioFrame() {
    std::lock_guard<std::mutex> lck(m_);
    auto r = audio_queue_.dequeue();
    cv_.notify_all();
    last_dequeue_pts_ = r.pts_;
    return r;
}

VideoFrame AVSync::dequeueVideoFrame() {
    std::unique_lock<std::mutex> lck(m_);
    cv_.wait(lck, [this] {
        auto video_frame = video_queue_.peek();
        return video_frame.pts_ <= last_dequeue_pts_;
    });
    auto r = video_queue_.dequeue();
    return r;
}

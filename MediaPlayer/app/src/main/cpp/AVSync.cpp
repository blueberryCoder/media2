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
    LOGI("enqueue audio frame pts:%lld", audioFrame.pts_);
    audio_queue_.enqueue(audioFrame);
}

void AVSync::enqueueVideoFrame(const VideoFrame &videoFrame) {
    LOGI("enqueue video frame pts:%lld", videoFrame.pts_);
    video_queue_.enqueue(videoFrame);
}

AudioFrame AVSync::dequeueAudioFrame() {
    std::lock_guard<std::mutex> lck(m_);
    auto r = audio_queue_.dequeue();
    LOGI("dequeue audio frame pts:%lld", r.pts_);
    cv_.notify_all();
    LOGI("dequeue audio notify lock");
    last_dequeue_pts_ = r.pts_;
    return r;
}

VideoFrame AVSync::dequeueVideoFrame() {
//    std::unique_lock<std::mutex> lck(m_);
    LOGI("dequeue video frame waiting lock,video queue size is %zu",video_queue_.size());
//    cv_.wait(lck, [this] {
//        auto video_frame = video_queue_.peek();
//        return video_frame.pts_ <= last_dequeue_pts_;
//    });
std::this_thread::sleep_for(std::chrono::milliseconds(60));
    auto r = video_queue_.dequeue();
    LOGI("dequeue video frame pts:%lld", r.pts_);
    return r;
}

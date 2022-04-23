//
// Created by bytedance on 2022/4/23.
//

#ifndef VIDEOPLAYER_SAFE_QUEUE_H
#define VIDEOPLAYER_SAFE_QUEUE_H

#include <queue>
#include <mutex>
#include <condition_variable>

// A threadsafe-queue
template <class T>
class SafeQueue
{
private:
    std::queue<T> q;
    mutable std::mutex m;
    std::condition_variable c;

public:
    SafeQueue(/* args */) : m(), q(), c()
    {
    }

    ~SafeQueue()
    {
    }

    void enqueue(T t)
    {
        std::lock_guard<std::mutex> lock(m);
        q.push(t);
        c.notify_all();
    }

    T dequeue(void)
    {
        std::unique_lock<std::mutex> lock(m);
        c.wait(lock, [this]
        { return !q.empty(); });
        T val = q.front();
        q.pop();
        return val;
    }
};
#endif //VIDEOPLAYER_SAFE_QUEUE_H

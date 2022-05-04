//
// Created by bytedance on 2022/4/23.
//

#ifndef VIDEOPLAYER_SAFE_QUEUE_H
#define VIDEOPLAYER_SAFE_QUEUE_H

#include <queue>
#include <mutex>
#include <condition_variable>
#include <vector>

// A threadsafe-queue
template<class T>
class SafeQueue {
private:
//    std::priority_queue<T, std::vector<T>, std::greater<T>> q;
    std::queue<T> q;
    mutable std::mutex m;
    std::condition_variable c;

public:
    SafeQueue(/* args */) : m(), q(), c() {
    }

    ~SafeQueue() {
    }

    SafeQueue<T> &operator=(const SafeQueue<T> ref) = delete;

    void enqueue(T t) {
        std::lock_guard<std::mutex> lock(m);
        q.push(t);
        c.notify_all();
    }

    T dequeue(void) {
        std::unique_lock<std::mutex> lock(m);
        c.wait(lock, [this] { return !q.empty(); });
//        T val = q.top();
        T val = q.front();
        q.pop();
        return val;
    }

    T peek(void) {
        std::unique_lock<std::mutex> lock(m);
        c.wait(lock, [this] { return !q.empty(); });
//        T val = q.top();
        T val = q.front();
        return val;
    }
    size_t  size(){return q.size();}
};

#endif //VIDEOPLAYER_SAFE_QUEUE_H

cmake_minimum_required(VERSION 3.6)
project(mediaplayer)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_BUILD_TYPE Debug)
add_library(mediaplayer SHARED
        app.cpp
        logger.h
        mediaplayerjni.cpp
        MediaPlayerController.cpp
        MediaPlayerController.h
        MyFormat.cpp
        MyFormat.h
        safe_queue.h
        AudioFrame.cpp
        AudioFrame.h
        VideoFrame.cpp
        VideoFrame.h
        AVSync.cpp
        AVSync.h
        GLRender.h
        GLRender.cpp
        )
target_link_libraries(mediaplayer log OpenSLES mediandk EGL GLESv3 GLESv2 android)
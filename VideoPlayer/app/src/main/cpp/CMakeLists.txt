cmake_minimum_required(VERSION 3.6)
project(videoplayer)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_BUILD_TYPE Debug)
add_library(videoplayer SHARED
        app.cpp
        logger.h
        jni_interfaces.h
        jni_interfaces.cpp
        audio_common.h
        audio_common.cpp
        buf_manager.h
        audio_effect.h
        audio_effect.cpp
        audio_player.h
        audio_player.cpp
        audio_recorder.h
        audio_recorder.cpp
        jni_play_mp3_interfaces.h
        jni_play_mp3_interfaces.cpp
        jni_mediacodec_audio.cpp
        safe_queue.h
        )
target_link_libraries(videoplayer log OpenSLES mediandk android)
cmake_minimum_required(VERSION 3.6.0)
# https://github.com/android/ndk-samples/blob/main/gles3jni/app/src/main/cpp/CMakeLists.txt
# https://developer.android.com/ndk/guides/stable_apis
project(glexample)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_BUILD_TYPE Debug)
# platform
# [18, 24)          ES2 & ES3
#                    If app is built to only support API-18 or later,
#                    set minimum api level to 18 is good enough, NDK supprts ES3
#                    with the right header and lib files. No need to use ES3 dynamic
#                    detection.
#   [24, infinite)   ES2 & ES3 & Vulkan

add_library(glejni SHARED app.cpp
        gl2jni.h
        gl2jni.cpp
        surfacejni.h
        surfacejni.cpp
        logger.h
        eglsurface.h
        eglsurface.cpp
        GLRender.h
        GLRender.cpp
        )
target_link_libraries(glejni
        log
        EGL
        m
        GLESv3
        GLESv2
        android # native window in this library
        )

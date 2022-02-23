cmake_minimum_required(VERSION 3.14)
project(ffmpeg-decoder)
set(CMAKE_CXX_STANDARD 11)            # Enable c++11 standard

set(CMAKE_BUILD_TYPE Debug)
set(SOURCE_FILES Demuxing.cpp Demuxing.h string_util.h App.cpp)

# https://cmake.org/cmake/help/latest/module/FindPkgConfig.html
# https://zhuanlan.zhihu.com/p/111155574

find_package(PkgConfig REQUIRED)
pkg_check_modules(libavformat REQUIRED IMPORTED_TARGET libavformat)
pkg_check_modules(libswscale REQUIRED IMPORTED_TARGET libswscale)
pkg_check_modules(libswresample REQUIRED IMPORTED_TARGET libswresample)
pkg_check_modules(libavutil REQUIRED IMPORTED_TARGET libavutil)

add_executable(ffmpeg-decoder ${SOURCE_FILES})

target_link_libraries(${PROJECT_NAME} PRIVATE PkgConfig::libavformat
        PkgConfig::libswscale PkgConfig::libswresample PkgConfig::libavutil
        )
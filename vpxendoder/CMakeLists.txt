cmake_minimum_required(VERSION 3.14)
project(Vpx_encoder)
set(CMAKE_CXX_STANDARD 11)            # Enable c++11 standard

set(CMAKE_BUILD_TYPE Debug)
set(SOURCE_FILES VpxEncoder.cpp)

# https://cmake.org/cmake/help/latest/module/FindPkgConfig.html
# https://zhuanlan.zhihu.com/p/111155574
find_package(PkgConfig REQUIRED)
pkg_check_modules(vpx REQUIRED IMPORTED_TARGET vpx )


add_executable(Vpx_encoder ${SOURCE_FILES})

target_link_libraries(${PROJECT_NAME} PRIVATE PkgConfig::vpx)
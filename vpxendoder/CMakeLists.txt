# cmake_minimum_required(VERSION 3.14)
# project(vpx_encoder)
# set(CMAKE_CXX_STANDARD 11)            # Enable c++11 standard

# set(CMAKE_BUILD_TYPE Debug)
# set(SOURCE_FILES VpxEncoder.cpp)
# message("$CMAKE_PROGRAM_PATH:" ${CMAKE_PROGRAM_PATH})
# # set(CMAKE_PROGRAM_PATH ${CMAKE_PROGRAM_PATH} /usr/local/lib)
# message("$CMAKE_PROGRAM_PATH:" ${CMAKE_PROGRAM_PATH})
# # https://cmake.org/cmake/help/latest/module/FindPkgConfig.html
# # https://zhuanlan.zhihu.com/p/111155574
# find_package(PkgConfig REQUIRED)
# pkg_check_modules(x265 REQUIRED IMPORTED_TARGET x265)

# add_executable(vpx_encoder ${SOURCE_FILES})

# target_link_libraries(${PROJECT_NAME} PRIVATE PkgConfig::x265)

cmake_minimum_required(VERSION 3.14)
project(x265_encoder)
set(CMAKE_CXX_STANDARD 11)            # Enable c++11 standard

set(CMAKE_BUILD_TYPE Debug)
set(SOURCE_FILES X265Encoder.cpp)
message("$CMAKE_PROGRAM_PATH:" ${CMAKE_PROGRAM_PATH})
# set(CMAKE_PROGRAM_PATH ${CMAKE_PROGRAM_PATH} /Users/bytedance/local/lib)
message("$CMAKE_PROGRAM_PATH:" ${CMAKE_PROGRAM_PATH})
# https://cmake.org/cmake/help/latest/module/FindPkgConfig.html
# https://zhuanlan.zhihu.com/p/111155574
find_package(PkgConfig REQUIRED)
pkg_check_modules(x265 REQUIRED IMPORTED_TARGET x265 )

add_executable(x265_encoder ${SOURCE_FILES})

target_link_libraries(${PROJECT_NAME} PRIVATE PkgConfig::x265)
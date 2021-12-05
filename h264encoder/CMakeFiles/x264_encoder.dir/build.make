# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.22

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:

#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:

# Disable VCS-based implicit rules.
% : %,v

# Disable VCS-based implicit rules.
% : RCS/%

# Disable VCS-based implicit rules.
% : RCS/%,v

# Disable VCS-based implicit rules.
% : SCCS/s.%

# Disable VCS-based implicit rules.
% : s.%

.SUFFIXES: .hpux_make_needs_suffix_list

# Command-line flag to silence nested $(MAKE).
$(VERBOSE)MAKESILENT = -s

#Suppress display of executed commands.
$(VERBOSE).SILENT:

# A target that is always out of date.
cmake_force:
.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /usr/local/Cellar/cmake/3.22.0/bin/cmake

# The command to remove a file.
RM = /usr/local/Cellar/cmake/3.22.0/bin/cmake -E rm -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /Users/bytedance/code/my/media/h264encoder

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /Users/bytedance/code/my/media/h264encoder

# Include any dependencies generated for this target.
include CMakeFiles/x264_encoder.dir/depend.make
# Include any dependencies generated by the compiler for this target.
include CMakeFiles/x264_encoder.dir/compiler_depend.make

# Include the progress variables for this target.
include CMakeFiles/x264_encoder.dir/progress.make

# Include the compile flags for this target's objects.
include CMakeFiles/x264_encoder.dir/flags.make

CMakeFiles/x264_encoder.dir/X264Encoder.cpp.o: CMakeFiles/x264_encoder.dir/flags.make
CMakeFiles/x264_encoder.dir/X264Encoder.cpp.o: X264Encoder.cpp
CMakeFiles/x264_encoder.dir/X264Encoder.cpp.o: CMakeFiles/x264_encoder.dir/compiler_depend.ts
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/bytedance/code/my/media/h264encoder/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building CXX object CMakeFiles/x264_encoder.dir/X264Encoder.cpp.o"
	/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -MD -MT CMakeFiles/x264_encoder.dir/X264Encoder.cpp.o -MF CMakeFiles/x264_encoder.dir/X264Encoder.cpp.o.d -o CMakeFiles/x264_encoder.dir/X264Encoder.cpp.o -c /Users/bytedance/code/my/media/h264encoder/X264Encoder.cpp

CMakeFiles/x264_encoder.dir/X264Encoder.cpp.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/x264_encoder.dir/X264Encoder.cpp.i"
	/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/bytedance/code/my/media/h264encoder/X264Encoder.cpp > CMakeFiles/x264_encoder.dir/X264Encoder.cpp.i

CMakeFiles/x264_encoder.dir/X264Encoder.cpp.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/x264_encoder.dir/X264Encoder.cpp.s"
	/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/bytedance/code/my/media/h264encoder/X264Encoder.cpp -o CMakeFiles/x264_encoder.dir/X264Encoder.cpp.s

# Object files for target x264_encoder
x264_encoder_OBJECTS = \
"CMakeFiles/x264_encoder.dir/X264Encoder.cpp.o"

# External object files for target x264_encoder
x264_encoder_EXTERNAL_OBJECTS =

x264_encoder: CMakeFiles/x264_encoder.dir/X264Encoder.cpp.o
x264_encoder: CMakeFiles/x264_encoder.dir/build.make
x264_encoder: /usr/local/Cellar/x264/r3060/lib/libx264.dylib
x264_encoder: CMakeFiles/x264_encoder.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --bold --progress-dir=/Users/bytedance/code/my/media/h264encoder/CMakeFiles --progress-num=$(CMAKE_PROGRESS_2) "Linking CXX executable x264_encoder"
	$(CMAKE_COMMAND) -E cmake_link_script CMakeFiles/x264_encoder.dir/link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
CMakeFiles/x264_encoder.dir/build: x264_encoder
.PHONY : CMakeFiles/x264_encoder.dir/build

CMakeFiles/x264_encoder.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles/x264_encoder.dir/cmake_clean.cmake
.PHONY : CMakeFiles/x264_encoder.dir/clean

CMakeFiles/x264_encoder.dir/depend:
	cd /Users/bytedance/code/my/media/h264encoder && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /Users/bytedance/code/my/media/h264encoder /Users/bytedance/code/my/media/h264encoder /Users/bytedance/code/my/media/h264encoder /Users/bytedance/code/my/media/h264encoder /Users/bytedance/code/my/media/h264encoder/CMakeFiles/x264_encoder.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : CMakeFiles/x264_encoder.dir/depend

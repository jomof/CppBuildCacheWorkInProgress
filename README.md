# Mostly From Scratch Guide to Android C++ Build
This repo describes the Android Gradle C++ build process from the perspective of 
someone who knows Gradle, Android Java build, and basics of C/C++ development but without
knowing the Android specific build aspects.

## Ninja, CMake, Gradle
This section describes the build tools we use from the lowest level (Ninja) to the 
highest (Gradle). 

### Ninja, the machine language of C++ builds
Ninja is a very low-level C++ build system. Its build files, usually named 'build.ninja' 
are expressly intended to be generated by a higher-level tool rather than hand maintained
by a human. In our case the higher-level tool is CMake. Ninja's responsibilities are:
a) To compose the clang++ flags for each expected .o file
b) To learn and remember the relationship between each .o file and corresponding .cpp
and .h files needed to build it
c) To understand which .o files are out of date
d) To build, with parallelization, just the .o files that are out of date

Ninja is widely recognized as the best tool for high-performance, parallel, incremental,
C/C++ build.

In order to achieve (b) Ninja invokes clang++ with the -E flag the first time a .o file 
is compiled from a .cpp. For subsequent builds, the -E flag is not used unless Ninja detects
a change in .cpp or .h files. These dependencies are stored in a proprietary format
called .ninja_deps.

The Ninja project is meant to be reused across clean calls so, in our case, it's stored
outside the normal build/ folder in a folder named .cxx/.

Resources:
- [ninja.org](https://ninja-build.org/)
- Ninja build files for our HelloWorld project:

  [HelloWorld/app/.cxx/Debug/3z5c3158/x86/build.ninja](https://github.com/jomof/CppBuildCacheWorkInProgress/blob/main/HelloWorld/app/.cxx/Debug/3z5c3158/x86/build.ninja)
  
  [HelloWorld/app/.cxx/Debug/3z5c3158/x86/CMakeFiles/rules.ninja](https://github.com/jomof/CppBuildCacheWorkInProgress/blob/main/HelloWorld/app/.cxx/Debug/3z5c3158/x86/CMakeFiles/rules.ninja)
- The (binary format) .o to .cpp and .h dependencies:
  [HelloWorld/app/.cxx/Debug/3z5c3158/x86/.ninja_deps](https://github.com/jomof/CppBuildCacheWorkInProgress/blob/main/HelloWorld/app/.cxx/Debug/3z5c3158/x86/.ninja_deps/native-lib.cpp.o)
- The location of the result .o: [HelloWorld/app/.cxx/Debug/3z5c3158/x86/CMakeFiles/native-lib.dir/native-lib.cpp.o](https://github.com/jomof/CppBuildCacheWorkInProgress/blob/main/HelloWorld/app/.cxx/Debug/3z5c3158/x86/CMakeFiles/native-lib.dir/native-lib.cpp.o)

### CMake, the meta C++ build-system
CMake is a C/C++ meta build-system. The term meta is used because CMake generates a build
system and doesn't directly compile .cpp to .o itself.
CMake's responsibilities are:
a) To have a maintainable, human-readable, representation of source files, flags, libraries 
   and their relationships with each other. This information is usually in a file called
   CMakeLists.txt.
b) To provide reflection on those build relationships so that IDEs can present the structure
   to users in a helpful way. This includes project structure tree features and language 
   service features like autocompletion.
In our case, CMake generates a Ninja project.

CMake's configuration phase is typically quite slow because it does things like test the
clang compiler for features and usually ends up invoking clang++.exe many times.

Resources:
- [CMake.org](https://cmake.org)
- User maintained build file: [HelloWorld/app/src/main/cpp/CMakeLists.txt](https://github.com/jomof/CppBuildCacheWorkInProgress/blob/main/HelloWorld/app/src/main/cpp/CMakeLists.txt)
- CMake-generated build reflection: [HelloWorld/app/.cxx/Debug/3z5c3158/x86/.cmake/api/v1/]((https://github.com/jomof/CppBuildCacheWorkInProgress/blob/main/HelloWorld/app/.cxx/Debug/3z5c3158/x86/.cmake/api/v1/))

### Android Gradle Plugin integrates C++ build with Java\Kotlin build
Android Gradle Plugin's C++ specific responsibilities are:
1) The "configure" (called externalNativeJsonGenerator) task invokes CMake to produce a set 
   of Ninja projects. There is one Ninja project per target ABI, per variant.
2) The "build" task execs ninja.exe for each relevant project.
The result of (2) is typically a set of .so files that are packaged into the final APK.

## Build Prerequisites
Android Gradle Plugin C/C++ has two prerequisites that typical Java\Kotlin
projects don't have.

1) NDK contains clang++.exe for Android along with headers and libraries.
   NDK version matters and HelloWorld project in this repo uses version 21.4.7075529

2) An external native build system. This project uses [CMake](cmake.org) but there
   is also a build system in the NDK specifically for Android called 'ndk-build'.
   The HelloWorld project uses version 3.18.1 of CMake.

Often, (1) and (2) will be downloaded and installed automatically during gradle build.

If this fails, then the fallback is to install Android Studio and, at the Welcome Screen,
select 'Configure' at lower right. The SDK Manager -> Appearance & Behavior -> System
Settings -> Android SDK -> SDK Tools. Click 'Show Package Details' in lower right and 
then find the required NDK version in the tree view under 'NDK (Side by side)'.

Repeat the process above for CMake 3.18.1.

## Build
In order to build:
```
cd HelloWorld
./gradlew assemble
```

# Mostly From Scratch Guide to Android C++ Build
This repo describes the Android Gradle C++ build process from the perspective of 
someone who knows Gradle, Android Java build, and basics of C/C++ development but without
knowing the Android specific build aspects.

## Prerequisites
Android Gradle Plugin C/C++ has two prerequites that typical Java\Kotlin
projects don't have.

1) NDK contains clang++.exe for Android along with headers and libraries.
   NDK version matters and HelloWorld project in this repo uses version 21.4.7075529

2) An external native build system. This project uses CMake[CMake.org] but there
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
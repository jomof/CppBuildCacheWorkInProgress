This file is generated by class com.android.build.gradle.internal.cxx.settings.SettingsDefaultConfigurationGoldenFileTest

This is the default configuration that will be used when the user has not specified a CMakeSettings configuration.
Any changes here will affect most C/C++ users.

```
{
  "environments": [],
  "configurations": [
    {
      "name": "traditional-android-studio-cmake-environment",
      "description": "Configuration generated by Android Gradle Plugin",
      "generator": "${ndk.moduleCmakeGenerator}",
      "configurationType": "${ndk.variantOptimizationTag}",
      "inheritEnvironments": [
        "ndk"
      ],
      "buildRoot": "${ndk.buildRoot}",
      "cmakeToolchain": "${ndk.cmakeToolchain}",
      "cmakeExecutable": "${ndk.moduleCmakeExecutable}",
      "variables": [
        {
          "name": "CMAKE_SYSTEM_NAME",
          "value": "Android"
        },
        {
          "name": "CMAKE_EXPORT_COMPILE_COMMANDS",
          "value": "ON"
        },
        {
          "name": "CMAKE_SYSTEM_VERSION",
          "value": "${ndk.platformSystemVersion}"
        },
        {
          "name": "ANDROID_PLATFORM",
          "value": "${ndk.platform}"
        },
        {
          "name": "ANDROID_ABI",
          "value": "${ndk.abi}"
        },
        {
          "name": "CMAKE_ANDROID_ARCH_ABI",
          "value": "${ndk.abi}"
        },
        {
          "name": "ANDROID_NDK",
          "value": "${ndk.moduleNdkDir}"
        },
        {
          "name": "CMAKE_ANDROID_NDK",
          "value": "${ndk.moduleNdkDir}"
        },
        {
          "name": "CMAKE_TOOLCHAIN_FILE",
          "value": "${ndk.cmakeToolchain}"
        },
        {
          "name": "CMAKE_MAKE_PROGRAM",
          "value": "${ndk.moduleNinjaExecutable}"
        },
        {
          "name": "CMAKE_C_FLAGS",
          "value": "${ndk.variantCFlags}"
        },
        {
          "name": "CMAKE_CXX_FLAGS",
          "value": "${ndk.variantCppFlags}"
        },
        {
          "name": "CMAKE_LIBRARY_OUTPUT_DIRECTORY",
          "value": "${ndk.soOutputDir}"
        },
        {
          "name": "CMAKE_RUNTIME_OUTPUT_DIRECTORY",
          "value": "${ndk.soOutputDir}"
        },
        {
          "name": "CMAKE_BUILD_TYPE",
          "value": "${ndk.variantOptimizationTag}"
        },
        {
          "name": "CMAKE_VERBOSE_MAKEFILE",
          "value": "${ndk.variantVerboseMakefile}"
        }
      ]
    }
  ]
}
```

/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.build.gradle.integration.nativebuild

import com.android.build.gradle.integration.common.fixture.GradleBuildResult
import com.android.build.gradle.integration.common.fixture.GradleTaskExecutor
import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.fixture.GradleTestProject.Companion.DEFAULT_BUILD_TOOL_VERSION
import com.android.build.gradle.integration.common.fixture.GradleTestProject.Companion.DEFAULT_COMPILE_SDK_VERSION
import com.android.build.gradle.integration.common.fixture.GradleTestProject.Companion.DEFAULT_NDK_SIDE_BY_SIDE_VERSION
import com.android.build.gradle.integration.common.fixture.app.HelloWorldJniApp
import com.android.build.gradle.integration.common.fixture.model.assertCacheHit
import com.android.build.gradle.integration.common.fixture.model.cartesianOf
import com.android.build.gradle.integration.common.fixture.model.enableCxxStructuredLogging
import com.android.build.gradle.integration.common.fixture.model.goldenBuildProducts
import com.android.build.gradle.integration.common.fixture.model.isLoadOrAttempt
import com.android.build.gradle.integration.common.fixture.model.isStoreOrAttempt
import com.android.build.gradle.integration.common.fixture.model.readStructuredLogs
import com.android.build.gradle.integration.common.fixture.model.recoverExistingCxxAbiModels
import com.android.build.gradle.integration.common.fixture.model.workspaceResourcePair
import com.android.build.gradle.integration.common.truth.TruthHelper.assertThat
import com.android.build.gradle.integration.common.utils.TestFileUtils
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.LOADED
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_LOADED_DEPENDENCIES_NOT_KNOWN
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_LOADED_KEY_DIDNT_EXIST
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_STORED_CACHE_ENTRY_ALREADY_EXISTED
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_STORED_BUILD_FAILED
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.STORED
import com.android.build.gradle.internal.cxx.caching.decodeObjectFileCacheEvent
import com.android.build.gradle.internal.cxx.configure.DEFAULT_CMAKE_VERSION
import com.android.build.gradle.internal.cxx.configure.OFF_STAGE_CMAKE_VERSION
import com.android.build.gradle.internal.cxx.logging.LoggingMessage.LoggingLevel.LIFECYCLE
import com.android.build.gradle.internal.cxx.logging.decodeLoggingMessage
import com.android.build.gradle.internal.cxx.logging.text
import com.android.build.gradle.internal.cxx.model.compileCommandsJsonBinFile
import com.android.build.gradle.options.StringOption.NATIVE_BUILD_OUTPUT_LEVEL
import com.android.utils.cxx.CxxDiagnosticCode
import com.android.utils.cxx.CxxDiagnosticCode.OBJECT_FILE_NOT_RESTORED_CHANGED_INPUTS
import com.android.utils.cxx.CxxDiagnosticCode.OBJECT_FILE_NOT_RESTORED_NO_DEPENDENCIES
import com.android.utils.cxx.CxxDiagnosticCode.OBJECT_FILE_RESTORED
import com.android.utils.cxx.streamCompileCommandsV2
import com.google.common.io.Resources
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class CMakeBuildCache(
    private val cmakeVersionInDsl: String,
    private val mode: Mode) {

    /**
     * This is the basic build cache test. It does a build, then a clean, then
     * another build. When build caching is enabled, this test checks that the
     * correct build cache events occurred and build outputs in the correct
     * location appear.
     */
    @Test
    fun `build, clean, build`() {
        enableCxxStructuredLogging(project)

        /** Build, clean, and then build should use the build cache for the second build */
        gradle("buildCMakeDebug", "clean")
        gradle("buildCMakeDebug")

        /** Validate cache events */
        val events = project.readStructuredLogs(::decodeObjectFileCacheEvent)
            .filter { event ->
                event.keyHashCode.startsWith("cxx/hello-jni.c.o/object/x86_64-android21")
            }

        ifCachingBuild {
            assertThat(events).hasSize(4)
            val (notLoaded, stored, loaded, notStored) = events
            assertCacheHit(stored, loaded)
            // Before first 'buildCMakeDebug' the .o file has not yet been cached
            assertThat(notLoaded.outcome).isEqualTo(NOT_LOADED_DEPENDENCIES_NOT_KNOWN)
            // After first 'buildCMakeDebug' the .o file is STORED to cache
            assertThat(stored.outcome).isEqualTo(STORED)
            // Before second 'buildCMakeDebug' the .o file is LOADED from cache
            assertThat(loaded.outcome).isEqualTo(LOADED)
            // After second 'buildCMakeDebug' we don't need to cache the .o because it was the same
            assertThat(notStored.outcome).isEqualTo(NOT_STORED_CACHE_ENTRY_ALREADY_EXISTED)
        }

        ifNotCachingBuild {
            assertThat(events).hasSize(0)
        }

        /** Build outputs should be the same regardless of caching or not */
        assertBuildProducts("""
            {PROJECT}/.cxx/{DEBUG}/x86_64/CMakeFiles/hello-jni.dir/src/main/cxx/hello-jni.c.o{F}
            {PROJECT}/build/intermediates/{DEBUG}/obj/x86_64/libhello-jni.so{F}
            """.trimIndent())

        checkStructuredLogsForAbsolutePaths()
    }

    /**
     * Build, edit .c file, and then build should not use the build cache for the
     * second build.
     */
    @Test
    fun `edit source file`() {
        enableCxxStructuredLogging(project)
        gradle("buildCMakeDebug")
        val source = project.buildFile.parentFile.resolve("src/main/cxx/hello-jni.c")
        assertThat(source.isFile).isTrue()
        ninjaDelay()
        source.writeText("""
            #include <string.h>
            #include <jni.h>

            // --> This is a trivial edit to the original file <---
            jstring
            Java_com_example_hellojni_HelloJni_stringFromJNI(JNIEnv* env, jobject thiz)
            {
                return (*env)->NewStringUTF(env, "hello world 2!");
            }
        """.trimIndent())
        gradle("buildCMakeDebug")

        /** Validate cache events */
        val events = project.readStructuredLogs(::decodeObjectFileCacheEvent)
            .filter { event ->
                event.keyHashCode.startsWith("cxx/hello-jni.c.o/object/x86_64-android21")
            }

        ifCachingBuild {
            assertThat(events).hasSize(4)
            val (notLoaded, stored, notLoaded2, stored2) = events
            // Before first 'buildCMakeDebug' the .o file has not yet been cached
            assertThat(notLoaded.outcome).isEqualTo(NOT_LOADED_DEPENDENCIES_NOT_KNOWN)
            // After first 'buildCMakeDebug' the .o file is STORED to cache
            assertThat(stored.outcome).isEqualTo(STORED)
            // Before second 'buildCMakeDebug' the .o file is not loaded because the .c changed
            assertThat(notLoaded2.outcome).isEqualTo(NOT_LOADED_KEY_DIDNT_EXIST)
            // After second 'buildCMakeDebug' the new .o is STORED to cache
            assertThat(stored2.outcome).isEqualTo(STORED)

            val messages = project.readStructuredLogs(::decodeLoggingMessage)
                .filter { message ->
                    message.diagnosticCode == OBJECT_FILE_NOT_RESTORED_CHANGED_INPUTS.reportCode
                }

            ifDebuggingBuildCache {
                assertThat(messages).hasSize(1)
                assertThat(messages[0].message).isEqualTo("""
                    [1/1] Object CMakeFiles/hello-jni.dir/src/main/cxx/hello-jni.c.o not restored from Gradle build cache because of change in ../../../../src/main/cxx/hello-jni.c
                """.trimIndent())
            }
        }

        ifNotCachingBuild {
            assertThat(events).hasSize(0)
        }

        /** Build outputs should be the same regardless of caching or not */
        assertBuildProducts("""
            {PROJECT}/.cxx/{DEBUG}/x86_64/CMakeFiles/hello-jni.dir/src/main/cxx/hello-jni.c.o{F}
            {PROJECT}/build/intermediates/{DEBUG}/obj/x86_64/libhello-jni.so{F}
            """.trimIndent())
    }

    /**
     * If there is a build error then we want to be sure to not cache
     * a stale result. Ninja appears to delete the built .o file if
     * there is an error in the corresponding .cpp. Build caching does
     * not depend on this behavior. Instead, any build failure results
     * in nothing being cached.
     */
    @Test
    fun `error in source file does not cache old object file`() {
        enableCxxStructuredLogging(project)
        gradle("buildCMakeDebug")
        val source = project.buildFile.parentFile.resolve("src/main/cxx/hello-jni.c")
        assertThat(source.isFile).isTrue()
        ninjaDelay()
        source.writeText("""
            invalid C/C++
        """.trimIndent())
        source.setLastModified(System.currentTimeMillis())
        val executor = gradleExecutor(project)
        executor.expectFailure()
        executor.run("buildCMakeDebug")

        /** Validate cache events */
        val events = project.readStructuredLogs(::decodeObjectFileCacheEvent)
            .filter { event ->
                event.keyHashCode.startsWith("cxx/hello-jni.c.o/object/x86_64-android21")
            }

        ifCachingBuild {
            assertThat(events).hasSize(4)
            val (notLoaded, stored, notLoaded2, notStored2) = events
            // Before first 'buildCMakeDebug' the .o file has not yet been cached
            assertThat(notLoaded.outcome).isEqualTo(NOT_LOADED_DEPENDENCIES_NOT_KNOWN)
            // After first 'buildCMakeDebug' the .o file is STORED to cache
            assertThat(stored.outcome).isEqualTo(STORED)
            // Before second 'buildCMakeDebug' the .o file is not loaded because the .c changed
            assertThat(notLoaded2.outcome).isEqualTo(NOT_LOADED_KEY_DIDNT_EXIST)
            // The second build's result is not stored because the build failed
            assertThat(notStored2.outcome).isEqualTo(NOT_STORED_BUILD_FAILED)

            val messages = project.readStructuredLogs(::decodeLoggingMessage)
                .filter { message ->
                    message.diagnosticCode == OBJECT_FILE_NOT_RESTORED_NO_DEPENDENCIES.reportCode
                }

            ifDebuggingBuildCache {
                // When debugging build cache, expect a message about no dependencies
                assertThat(messages).hasSize(1)
            }

            ifNotDebuggingBuildCache {
                // When debugging build cache, expect a no message about missing dependencies
                assertThat(messages).hasSize(0)
            }
        }

        ifNotCachingBuild {
            assertThat(events).hasSize(0)
        }

        /** Build outputs should be the same regardless of caching or not */
        assertBuildProducts("""
            {PROJECT}/build/intermediates/{DEBUG}/obj/x86_64/libhello-jni.so{F}
            """.trimIndent())
    }

    /**
     * Normally, ninja build output is not logged to stdout. However, it is if the user
     * passes -Pandroid.native.buildOutput=verbose to the build. In this case, the user
     * will see messages like:
     *
     *  [1/885] Building CXX object CMakeFiles/CARD.cpp.o
     *
     * For parity, we'd like to see a similar message when that same file is restored
     * from the Gradle build cache. So, for example:
     *
     *  [1/885] Restoring object CMakeFiles/CARD.cpp.o from Gradle build cache
     */
    @Test
    fun `restore messages logged when ninja output logged`() {
        enableCxxStructuredLogging(project)

        /** Build, clean, and then build should use the build cache for the second build */
        gradle(project, "buildCMakeDebug", "clean")
        val mark = project.readStructuredLogs(::decodeLoggingMessage).count()
        gradleExecutor(project)
            .with(NATIVE_BUILD_OUTPUT_LEVEL, "verbose")
            .run("buildCMakeDebug")

        /** Validate cache events */
        val messages = project.readStructuredLogs(::decodeLoggingMessage)
            .drop(mark)
            .filter { message ->
                message.diagnosticCode == OBJECT_FILE_RESTORED.reportCode &&
                        message.level == LIFECYCLE
            }

        ifCachingBuild {
            assertThat(messages).hasSize(1) // One file restored
        }

        ifNotCachingBuild {
            // No restore messages when not using build cache
            assertThat(messages).hasSize(0)
        }

        /** Build outputs should be the same regardless of caching or not */
        assertBuildProducts("""
            {PROJECT}/.cxx/{DEBUG}/x86_64/CMakeFiles/hello-jni.dir/src/main/cxx/hello-jni.c.o{F}
            {PROJECT}/build/intermediates/{DEBUG}/obj/x86_64/libhello-jni.so{F}
            """.trimIndent())
    }

    // This is the counterpart to the test above where logging is not verbose
    @Test
    fun `restore messages not logged when ninja output not logged`() {
        enableCxxStructuredLogging(project)

        /** Build, clean, and then build should use the build cache for the second build */
        gradle("buildCMakeDebug", "clean")
        val mark = project.readStructuredLogs(::decodeLoggingMessage).count()
        gradle("buildCMakeDebug")

        /** Validate cache events */
        val messages = project.readStructuredLogs(::decodeLoggingMessage)
            .drop(mark) // Skip any messages before second build
            .filter { message ->
                message.diagnosticCode == OBJECT_FILE_RESTORED.reportCode &&
                        message.level == LIFECYCLE
            }

        // A message is still logged if build cache debugging is enabled
        ifDebuggingBuildCache {
            assertThat(messages).hasSize(1)
        }

        // Otherwise, not message should be logged
        ifNotDebuggingBuildCache {
            assertThat(messages).hasSize(0)
        }

        /** Build outputs should be the same regardless of caching or not */
        assertBuildProducts("""
            {PROJECT}/.cxx/{DEBUG}/x86_64/CMakeFiles/hello-jni.dir/src/main/cxx/hello-jni.c.o{F}
            {PROJECT}/build/intermediates/{DEBUG}/obj/x86_64/libhello-jni.so{F}
            """.trimIndent())
    }

    /**
     * When one target is built, other targets should not be restored from the
     * build cache even if they are known to the cache.
     */
    @Test
    fun `build only for target`() {
        enableCxxStructuredLogging(project)
        val original = project.buildFile.parentFile.resolve("src/main/cxx/hello-jni.c")
        original.copyTo(original.resolveSibling("hello-jni-a.c"))
        original.copyTo(original.resolveSibling("hello-jni-b.c"))
        original.delete()
        val cmakeLists = project.buildFile.resolveSibling("CMakeLists.txt")
        cmakeLists.writeText("""
            cmake_minimum_required(VERSION 3.4.1)
            file(GLOB_RECURSE TARGETA src/main/cxx/hello-jni-a.c)
            file(GLOB_RECURSE TARGETB src/main/cxx/hello-jni-b.c)
            set(CMAKE_VERBOSE_MAKEFILE ON)
            add_library(target-a SHARED ${'$'}{TARGETA})
            add_library(target-b SHARED ${'$'}{TARGETB})
            target_link_libraries(target-a log)
            target_link_libraries(target-b log)
            """.trimIndent())
        TestFileUtils.appendToFile(
            project.buildFile,
            """
            android.defaultConfig.externalNativeBuild.cmake.abiFilters.clear()
            android.defaultConfig.externalNativeBuild.cmake.abiFilters "x86_64"
            android.buildTypes {
              debugA.externalNativeBuild.cmake.targets "target-a"
              debugB.externalNativeBuild.cmake.targets "target-b"
            }
            """.trimIndent())
        gradle("buildCMakeDebug", "clean")
        val mark = project.readStructuredLogs(::decodeObjectFileCacheEvent).count()
        gradle("buildCMakeDebug[target-a]")

        /** Validate cache events */
        val events = project.readStructuredLogs(::decodeObjectFileCacheEvent)
            .drop(mark)
            .filter { event -> event.outcome == LOADED }

        ifCachingBuild {
            // Only target-a .o file should be restored from build cache
            assertThat(events.map { it.keyHashCode }).hasSize(1)
        }

        ifNotCachingBuild {
            assertThat(events.map { it.keyHashCode }).hasSize(0)
        }

        /** Build outputs should be the same regardless of caching or not */
        assertBuildProducts("""
            {PROJECT}/.cxx/{DEBUG}/x86_64/CMakeFiles/target-a.dir/src/main/cxx/hello-jni-a.c.o{F}
            {PROJECT}/build/intermediates/{DEBUG}/obj/x86_64/libtarget-a.so{F}
            """.trimIndent())
    }

    /**
     * A single source .cpp can produce multiple .o files (because there are multiple targets).
     * This test reproduces this case and ensures that only the target-relevant .o file is
     * restored.
     */
    @Test
    fun `one source file used by two targets`() {
        enableCxxStructuredLogging(project)
        val cmakeLists = project.buildFile.resolveSibling("CMakeLists.txt")
        cmakeLists.writeText("""
            cmake_minimum_required(VERSION 3.4.1)
            file(GLOB_RECURSE TARGETA src/main/cxx/hello-jni.c)
            file(GLOB_RECURSE TARGETB src/main/cxx/hello-jni.c)
            set(CMAKE_VERBOSE_MAKEFILE ON)
            add_library(target-a SHARED ${'$'}{TARGETA})
            add_library(target-b SHARED ${'$'}{TARGETB})
            target_link_libraries(target-a log)
            target_link_libraries(target-b log)
            """.trimIndent())
        TestFileUtils.appendToFile(
            project.buildFile,
            """
            android.defaultConfig.externalNativeBuild.cmake.abiFilters.clear()
            android.defaultConfig.externalNativeBuild.cmake.abiFilters "x86_64"
            android.buildTypes {
              debugA.externalNativeBuild.cmake.targets "target-a"
              debugB.externalNativeBuild.cmake.targets "target-b"
            }
            """.trimIndent())
        gradle("buildCMakeDebug", "clean")
        val mark = project.readStructuredLogs(::decodeObjectFileCacheEvent).count()
        gradle("buildCMakeDebug[target-a]")

        /** Validate cache events */
        val events = project.readStructuredLogs(::decodeObjectFileCacheEvent)
            .drop(mark)
            .filter { event -> event.outcome == LOADED }

        ifCachingBuild {
            // Only target-a .o file should be restored from build cache
            assertThat(events.map { it.keyHashCode }).hasSize(1)
        }

        ifNotCachingBuild {
            assertThat(events.map { it.keyHashCode }).hasSize(0)
        }

        /** Build outputs should be the same regardless of caching or not */
        assertBuildProducts("""
            {PROJECT}/.cxx/{DEBUG}/x86_64/CMakeFiles/target-a.dir/src/main/cxx/hello-jni.c.o{F}
            {PROJECT}/build/intermediates/{DEBUG}/obj/x86_64/libtarget-a.so{F}
            """.trimIndent())
    }

    /**
     * Rename a source file. This will be a cache miss but it could be converted to
     * a cache hit at some point in the future. In the meantime, just use it to make
     * sure there's no crash since the new source file won't be present in the
     * pre-existing .ninja_deps.
     */
    @Test
    fun `rename source file`() {
        enableCxxStructuredLogging(project)
        /** Build, edit .c file, and then build should not use the build cache for the second build */
        gradle("buildCMakeDebug", "clean")
        val old = project.buildFile.parentFile.resolve("src/main/cxx/hello-jni.c")
        val new = project.buildFile.parentFile.resolve("src/main/cxx/hello-jni-2.c")
        old.copyTo(new)
        old.delete()
        val cmakeLists = project.buildFile.resolveSibling("CMakeLists.txt")
        cmakeLists.writeText("""
            cmake_minimum_required(VERSION 3.4.1)
            file(GLOB_RECURSE SRC src/main/cxx/hello-jni-2.c)
            set(CMAKE_VERBOSE_MAKEFILE ON)
            add_library(hello-jni SHARED ${'$'}{SRC})
            target_link_libraries(hello-jni log)
            """.trimIndent())
        gradle("buildCMakeDebug")

        /** Build outputs should be the same regardless of caching or not */
        assertBuildProducts("""
            {PROJECT}/.cxx/{DEBUG}/x86_64/CMakeFiles/hello-jni.dir/src/main/cxx/hello-jni-2.c.o{F}
            {PROJECT}/build/intermediates/{DEBUG}/obj/x86_64/libhello-jni.so{F}
            """.trimIndent())
    }

    /**
     * This test validates that compile_commands.json.bin contains target information
     * for each source file. This is a prerequisite for build caching.
     */
    @Test
    fun `compile commands bin has target information`() {
        enableCxxStructuredLogging(project)
        gradle("generateJsonModelDebug")
        val abis = project.recoverExistingCxxAbiModels()
        assertThat(abis).hasSize(1) // One variant * one abi

        /** Check each compile_command.json.bin */
        for(abi in abis) {
            streamCompileCommandsV2(abi.compileCommandsJsonBinFile) {
                assertThat(target).isEqualTo("hello-jni")
                assertThat(sourceFileCount).isEqualTo(1)
            }
        }
    }

    /**
     * Check the case where one of the source files has an absolute path.
     * In this case, CMake constructs a special location for the .o that
     * contains the full path to the original .c or .cpp.
     */
    @Test
    fun `source file with absolute path`() {
        enableCxxStructuredLogging(project)
        val cmakeLists = project.buildFile.resolveSibling("CMakeLists.txt")
        cmakeLists.writeText("""
            cmake_minimum_required(VERSION 3.4.1)
            set(APP_GLUE_DIR ${'$'}{ANDROID_NDK}/sources/android/native_app_glue)
            include_directories($${'$'}{APP_GLUE_DIR})
            add_library(app-glue STATIC ${'$'}{APP_GLUE_DIR}/android_native_app_glue.c)
            file(GLOB_RECURSE SRC src/main/cxx/hello-jni.c)
            set(CMAKE_VERBOSE_MAKEFILE ON)
            add_library(hello-jni SHARED ${'$'}{SRC})
            target_link_libraries(hello-jni log app-glue)
            """.trimIndent())
        gradle("buildCMakeDebug", "clean")
        val mark = project.readStructuredLogs(::decodeObjectFileCacheEvent).count()
        gradle("buildCMakeDebug")

        /** Validate cache events */
        val events = project.readStructuredLogs(::decodeObjectFileCacheEvent)
            .drop(mark)
            .filter { it.outcome == LOADED }
            .sortedBy { it.keyHashCode }
            .distinct()

        ifCachingBuild {
            assertThat(events).hasSize(2)
            val (loadedGlue, loadedHello) = events
            assertThat(loadedGlue.keyHashCode.startsWith("cxx/android_native_app_glue.c"))
                .named(loadedGlue.keyHashCode)
                .isTrue()
            assertThat(loadedHello.keyHashCode.startsWith("cxx/hello-jni.c"))
                .named(loadedHello.keyHashCode)
                .isTrue()
        }

        ifNotCachingBuild {
            assertThat(events).hasSize(0)
        }
    }

    @Test
    fun `cache results are shared between different projects`() {
        enableCxxStructuredLogging(project)
        enableCxxStructuredLogging(project2)
        gradle(project, "buildCMakeDebug", "clean")
        gradle(project2, "buildCMakeDebug", "clean")

        val user1SourceFile = project.buildFile.parentFile.resolve("src/main/cxx/hello-jni.c")
        val user2SourceFile = project2.buildFile.parentFile.resolve("src/main/cxx/hello-jni.c")

        // User 1 edits a source file
        user1SourceFile.writeText("""
            #include <string.h>
            #include <jni.h>

            // --> This is a trivial edit to the original file <---
            jstring
            Java_com_example_hellojni_HelloJni_stringFromJNI(JNIEnv* env, jobject thiz)
            {
                return (*env)->NewStringUTF(env, "hello world!");
            }
        """.trimIndent())
        val mark = project.readStructuredLogs(::decodeObjectFileCacheEvent).count()
        gradle(project, "buildCMakeDebug")

        // User 1 uploads change to source control and user 2 syncs it
        user1SourceFile.copyTo(user2SourceFile, overwrite = true)

        // User 2 builds
        gradle(project2, "buildCMakeDebug")

        val user1CacheEvents = project.readStructuredLogs(::decodeObjectFileCacheEvent)
            .drop(mark)

        val user2CacheEvents = project2.readStructuredLogs(::decodeObjectFileCacheEvent)
            .drop(mark)

        ifCachingBuild {
            val store = user1CacheEvents.single { it.outcome.isStoreOrAttempt }
            val load = user2CacheEvents.single { it.outcome.isLoadOrAttempt }
            assertCacheHit(store, load)
        }
    }

    /**
     * This test confirms a checked-in build cache hash code remains the same
     * across time and across different host OSes.
     */
    @Test
    fun `cache results are shared between different host OSes`() {
        assumeTrue(mode == Mode.CACHING)
        enableCxxStructuredLogging(project)

        val (infoResource, infoFile) =
            workspaceResourcePair<CMakeBuildCache>("cross-os-check-info.txt")

        gradle("buildCMakeDebug", "clean")
        val actualStoreEvent = project.readStructuredLogs(::decodeObjectFileCacheEvent)
            .filter { event -> event.outcome == STORED }
            .single()

        val actualKeyHash = actualStoreEvent.keyHashCode
        val expectedKeyHash = Resources.readLines(Resources.getResource(infoResource), Charsets.UTF_8)[1]

        if (actualKeyHash != expectedKeyHash) {
            val message = "# C/C++ build hash is expected to change when NDK version is updated, but it should not vary between host OSes"
            val expectedInfo = """
                $message
                $actualKeyHash
            """.trimIndent()
            infoFile.writeText(expectedInfo)
            assertThat(actualKeyHash)
                .named(message)
                .isEqualTo(expectedKeyHash)
        }
    }

    private val cacheFolder = File("../fresh-local-build-cache")

    @Rule
    @JvmField
    val project = GradleTestProject.builder()
        .fromTestApp(HelloWorldJniApp.builder().withNativeDir("cxx").withCmake().build())
        .withGradleBuildCacheDirectory(cacheFolder)
        .setSideBySideNdkVersion(DEFAULT_NDK_SIDE_BY_SIDE_VERSION)
        .setWithNdkSymlinkDirInLocalProp("proj1-ndk")
        .withName("project")
        .create()


    @Rule
    @JvmField
    val project2 = GradleTestProject.builder()
        .fromTestApp(HelloWorldJniApp.builder().withNativeDir("cxx").withCmake().build())
        .withGradleBuildCacheDirectory(cacheFolder)
        .setSideBySideNdkVersion(DEFAULT_NDK_SIDE_BY_SIDE_VERSION)
        .setWithNdkSymlinkDirInLocalProp("proj2-ndk")
        .withName("project2")
        .create()

    @Before
    fun setUp() {
        TestFileUtils.appendToFile(project.buildFile, moduleBody())
        TestFileUtils.appendToFile(project2.buildFile, moduleBody())
        val cacheFolder = project.buildFile.parentFile.resolve(cacheFolder)
        if (cacheFolder.exists()) {
            cacheFolder.deleteRecursively()
        }
    }

    @After
    fun after() {
        // Absolute paths break caching between projects.
        // Do a check to try to identify absolute paths in cache keys.
        checkStructuredLogsForAbsolutePaths()
    }

    private fun moduleBody() : String = ("""
        apply plugin: 'com.android.application'
        android {
            compileSdkVersion $DEFAULT_COMPILE_SDK_VERSION
            buildToolsVersion "$DEFAULT_BUILD_TOOL_VERSION"
            ndkPath "${project.ndkPath}"
            defaultConfig.externalNativeBuild.cmake.abiFilters "x86_64"
            externalNativeBuild.cmake.path "CMakeLists.txt"
            externalNativeBuild.cmake.version "$cmakeVersionInDsl"
        }
    """.trimIndent())

    private fun <T> ifCachingBuild(action:() -> T?) = if (mode == Mode.CACHING || mode == Mode.CACHING_AND_DEBUGGING) action() else null
    private fun <T> ifNotCachingBuild(action:() -> T?) = if (mode == Mode.NOT_CACHING) action() else null
    private fun <T> ifDebuggingBuildCache(action:() -> T?) = if (mode == Mode.CACHING_AND_DEBUGGING) action() else null
    private fun <T> ifNotDebuggingBuildCache(action:() -> T?) = if (mode != Mode.CACHING_AND_DEBUGGING) action() else null

    /**
     * The coarsest clock resolution in .ninja_deps is one second.
     * When editing a code source file in a test we need to wait a bit or
     * Ninja won't recognize the change.
     */
    private fun ninjaDelay() {
        Thread.sleep(1000)
    }

    /**
     * Assert that build outputs match [expect]
     */
    private fun assertBuildProducts(expect : String) {
        val golden = project.goldenBuildProducts()
        if (golden != expect) {
            println(golden)
            Truth.assertThat(golden).isEqualTo(expect)
        }
    }

    /**
     * Helper function that controls arguments when running a task
     */
    private fun gradleExecutor(project : GradleTestProject, vararg params : String): GradleTaskExecutor {
        val (args, _) = params.partition { it.startsWith("-") }
        val executor = project.executor().withArguments(args)
        ifDebuggingBuildCache {
            executor.withArgument("-Dorg.gradle.caching.debug=true")
        }
        ifCachingBuild {
            executor.withArgument("--build-cache")
        }
        return executor
    }

    /**
     * Invoke gradle with [params]
     */
    private fun gradle(vararg params : String): GradleBuildResult {
        return gradle(project, *params)
    }

    /**
     * Invoke gradle with [params]
     */
    private fun gradle(project : GradleTestProject, vararg params : String): GradleBuildResult {
        val (_, tasks) = params.partition { it.startsWith("-") }
        return gradleExecutor(project, *params).run(tasks)
    }

    private fun checkStructuredLogsForAbsolutePaths() {
        checkStructuredLogsForAbsolutePaths(
            project.readStructuredLogs(::decodeObjectFileCacheEvent)
        )
        checkStructuredLogsForAbsolutePaths(
            project2.readStructuredLogs(::decodeObjectFileCacheEvent)
        )
    }

    private fun checkStructuredLogsForAbsolutePaths(events: List<ObjectFileCacheEvent>) {
        // Find the second level parent folder name
        val original = File(".").absoluteFile
        var rootSegment = original
        while(true) {
            val parent = rootSegment.parentFile?.parentFile ?: break
            rootSegment = parent.parentFile ?:
                error("Could not find root segments of [$original]")
        }
        for(event in events) {
            for(flag in event.hashedCompilation.objectFileKey.dependencyKey.compilerFlagsList) {
                assertThat(!flag.contains(rootSegment.path))
                    .named("flag $flag looks like it has an absolute path because it contains [$rootSegment]")
                    .isTrue()
            }
        }
    }

    companion object {
        enum class Mode {
            NOT_CACHING,
            CACHING,
            CACHING_AND_DEBUGGING
        }
        @Parameterized.Parameters(name = "cmake={0} build-cache-mode={1}")
        @JvmStatic
        fun data() = cartesianOf(
            arrayOf("3.6.0", OFF_STAGE_CMAKE_VERSION, DEFAULT_CMAKE_VERSION),
            Mode.values()
        )
    }
}

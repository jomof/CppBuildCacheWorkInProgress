/*
 * Copyright (C) 2017 The Android Open Source Project
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

import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.fixture.GradleTestProject.Companion.DEFAULT_NDK_SIDE_BY_SIDE_VERSION
import com.android.build.gradle.integration.common.truth.TruthHelper.assertThat
import com.android.build.gradle.internal.cxx.configure.DEFAULT_CMAKE_SDK_DOWNLOAD_VERSION
import com.android.build.gradle.internal.cxx.configure.DEFAULT_CMAKE_VERSION
import com.android.build.gradle.internal.cxx.configure.OFF_STAGE_CMAKE_VERSION
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class VulkanTest(
    private val cmakeVersionInDsl: String) {
    companion object {
        @Parameterized.Parameters(name = "cmake={0}")
        @JvmStatic
        fun data() = arrayOf("3.6.0", OFF_STAGE_CMAKE_VERSION, DEFAULT_CMAKE_VERSION)
    }

    @get:Rule
    val project = GradleTestProject.builder()
        .setSideBySideNdkVersion(DEFAULT_NDK_SIDE_BY_SIDE_VERSION)
        .fromTestProject("vulkan").create()

    @Before
    fun setUp() {
        val buildGradleFile = project.buildFile.resolveSibling("build.gradle")
        buildGradleFile.writeText(
            buildGradleFile
            .readText()
            .replace("{CMAKE_VERSION}", cmakeVersionInDsl))
    }

    @Test
    fun `check basic Vulkan project`() {
        project.executor().run("assembleDebug")

        project.getApk(GradleTestProject.ApkType.DEBUG).use { apk ->
            assertThat(apk).containsFile("lib/x86/libvktuts.so")
            assertThat(apk).containsFile("lib/x86_64/libvktuts.so")
            assertThat(apk).containsFile("lib/armeabi-v7a/libvktuts.so")
            assertThat(apk).containsFile("lib/arm64-v8a/libvktuts.so")
            assertThat(apk).containsFile("assets/shaders/tri.vert.spv")
            assertThat(apk).containsFile("assets/shaders/tri.frag.spv")
        }
    }
}

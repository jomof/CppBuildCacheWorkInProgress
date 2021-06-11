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

package com.android.build.gradle.internal.cxx.caching

import com.android.repository.Revision
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class BuildCacheKeyMakerTest {
    @Test
    fun `check basic functionality`() {
        val keyMaker = BuildCacheKeyMaker(
            ndkVersion = Revision.parseRevision("1.2.3"),
            ndkFolder = File("ndk-folder")
        ) { file -> "hash: $file" }
        val cacheKeys = keyMaker.makeKeys(
            File("source-file"),
            listOf("flag1", "flag2"),
            File("working-directory"),
            File("output-file"),
            listOf("dependency-1", "dependency-2")
        )

        assertThat(cacheKeys.objectKey.displayName).isEqualTo("compile source-file to output-file")
        assertThat(cacheKeys.objectKey.hashCode).isEqualTo("cxx/output-file/object/e092c1643904972eda882401c5099d09")
        assertThat(cacheKeys.explanationKey.displayName).isEqualTo("compile source-file to output-file (explanation)")
        assertThat(cacheKeys.explanationKey.hashCode).isEqualTo("cxx/output-file/explain/8438b138daee79056dc19691f7f667e1")
    }

    @Test
    fun `NDK path is replaced by version`() {
        val keyMaker = BuildCacheKeyMaker(
            ndkVersion = Revision.parseRevision("1.2.3"),
            ndkFolder = File("ndk-folder")
        ) { file -> "hash: $file" }
        val cacheKeys = keyMaker.makeKeys(
            File("source-file"),
            listOf("ndk-folder/clang.exe", "flag2"),
            File("working-directory"),
            File("output-file"),
            listOf("dependency-1", "dependency-2")
        )

        assertThat(cacheKeys.hashedCompilation.objectFileKey.dependencyKey.getCompilerFlags(0))
            .isEqualTo("<NDK:1.2.3>/clang.exe")
    }
}

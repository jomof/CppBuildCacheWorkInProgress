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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class BuildCacheKeysTest {
    @Test
    fun `basic functionality`() {
        val workingDirectory = File("working-directory")
        val sourceFile = File("source-file")
        val outputFile = File("output-file")
        val flags = listOf("flag1", "flag2")
        val dependencies = listOf("dependency-1", "dependency-2")
        val compilation = Compilation.newBuilder()
            .setWorkingDirectory(workingDirectory.path)
            .setObjectFileKey(
                ObjectFileKey.newBuilder()
                    .setDependencyKey(
                        DependenciesKey.newBuilder()
                            .setSourceFile(sourceFile.path)
                            .addAllCompilerFlags(flags))
                    .addAllDependencies(dependencies))
            .setObjectFile(outputFile.path)
            .build()

        val hashedCompilation = Compilation.newBuilder()
            .setWorkingDirectory("hash: $workingDirectory")
            .setObjectFileKey(
                ObjectFileKey.newBuilder()
                    .setDependencyKey(
                        DependenciesKey.newBuilder()
                            .setSourceFile("hash: $sourceFile")
                            .addAllCompilerFlags(flags.map { "hash: $it" }))
                    .addAllDependencies(dependencies.map { "hash: $it" }))
            .setObjectFile("hash: $outputFile.path")
            .build()

        val cacheKeys = BuildCacheKeys(
            clangKeySegment = "clang-segment/",
            compilation = compilation,
            hashedCompilation = hashedCompilation,
            hasKnownDependencies = true
        )
        assertThat(cacheKeys.objectKey.displayName).isEqualTo("compile source-file to output-file")
        assertThat(cacheKeys.objectKey.hashCode).isEqualTo("cxx/output-file/object/clang-segment/d0fe92fd7cadf5e06e3f57c83cdded53")
        assertThat(cacheKeys.explanationKey.displayName).isEqualTo("compile source-file to output-file (explanation)")
        assertThat(cacheKeys.explanationKey.hashCode).isEqualTo("cxx/output-file/explain/clang-segment/8438b138daee79056dc19691f7f667e1")
    }
}

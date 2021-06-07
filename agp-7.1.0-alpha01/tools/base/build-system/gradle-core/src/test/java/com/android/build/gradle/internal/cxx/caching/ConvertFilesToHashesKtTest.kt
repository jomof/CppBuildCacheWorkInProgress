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

class ConvertFilesToHashesKtTest {
//    @Test
//    fun `check convert files to hashes`() {
//        val original = Compilation.newBuilder()
//            .setWorkingDirectory("working-directory")
//            .setObjectFile("object-file")
//            .setObjectFileKey(ObjectFileKey.newBuilder()
//                .addAllDependencies(listOf("dependency-1", "dependency-2"))
//                .setDependencyKey(DependenciesKey.newBuilder()
//                    .setSourceFile("source-file")
//                    .addAllCompilerFlags(listOf("flag-1", "flag-2"))
//                )
//            ).build()
//        val expected = Compilation.newBuilder()
//            .setObjectFile("<no-object-file-existed>")
//            .setObjectFileKey(ObjectFileKey.newBuilder()
//                .addAllDependencies(listOf(
//                    "hash: working-directory/dependency-1",
//                    "hash: working-directory/dependency-2"))
//                .setDependencyKey(DependenciesKey.newBuilder()
//                    .setSourceFile("hash: working-directory/source-file")
//                    .addAllCompilerFlags(listOf("flag-1", "flag-2"))
//                )
//            ).build()
//        val actual = original.convertFilesToHashes { file ->
//            "hash: $file".replace("\\", "/")
//        }
//        assertThat(actual).isEqualTo(expected)
//    }
}

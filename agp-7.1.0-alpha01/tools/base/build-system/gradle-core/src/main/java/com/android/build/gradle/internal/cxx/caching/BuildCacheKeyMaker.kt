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
import java.io.File

/**
 * Produces [BuildCacheKeys] for a particular set of compile flags, source
 * file, and list of dependencies.
 */
class BuildCacheKeyMaker(
    private val ndkVersion : Revision,
    private val ndkFolder : File,
    private val hashFile: (File) -> String
) {
    /**
     * Maintains a list of raw flags from compile_commands.json to
     * flags that have been normalized to be transportable between
     * projects and machines.
     *
     * The key in this map comes directly from compile_commands.json.bin.
     * This source guarantees that identical flags (List<String>) are
     * equal by reference identity.
     */
    private val remotedFlags = mutableMapOf<List<String>, List<String>>()

    /**
     * Cache of conversion from list of flags to key segement produced
     * by [ClangKeySegment]
     */
    private val keySegmentMap = mutableMapOf<List<String>, String>()

    /**
     * Create [BuildCacheKeys] for this set of [sourceFile] and [flags].
     *
     */
    fun makeKeys(
        sourceFile: File,
        flags: List<String>,
        workingDirectory: File,
        outputFile: File,
        dependenciesOrNull: List<String>?) : BuildCacheKeys {

        val dependencies = dependenciesOrNull ?: listOf()
        val objectFilePath = workingDirectory.resolve(outputFile)
        val hashedObjectFile = if (objectFilePath.isFile) {
            hashFile(objectFilePath)
        } else "<no-object-file-existed>"
        val hashedDependencies = dependencies.map { path ->
            hashFile(workingDirectory.resolve(path))
        }

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
            .setObjectFileKey(
                ObjectFileKey.newBuilder()
                    .setDependencyKey(
                        DependenciesKey.newBuilder()
                            .setSourceFile(hashFile(workingDirectory.resolve(sourceFile)))
                            .addAllCompilerFlags(remoteFlags(flags)))
                    .addAllDependencies(hashedDependencies))
            .setObjectFile(hashedObjectFile)
            .build()

        return BuildCacheKeys(
            clangKeySegment = keySegmentMap.computeIfAbsent(flags, ::computeClangKeySegment),
            compilation = compilation,
            hashedCompilation = hashedCompilation,
            hasKnownDependencies = dependenciesOrNull != null
        )
    }

    /**
     * Convert a clang flags to a format that is transportable between
     * projects on different projects, including projects on different
     * machines.
     */
    private fun remoteFlags(flags : List<String>) : List<String> {
        return remotedFlags.computeIfAbsent(flags) {
            flags.map { it.replace(ndkFolder.path, "<NDK:$ndkVersion>") }
        }
    }
}

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

import com.android.build.gradle.internal.cxx.model.CxxAbiModel
import org.gradle.caching.internal.controller.BuildCacheController
import org.gradle.internal.hash.FileHasher
import java.io.File

/**
 * C/C++ build cache implementation.
 *
 * Currently:
 * - cache the result of compiling a .c or .cpp to object file
 * - only for CMake\ninja
 * - restore only files where we can determine #include dependencies
 *   without calling clang ourself (see NOTE 1)
 *
 * Does not, but possibly could:
 * - support ndk-build (see NOTE 2)
 * - cache .o -> .a (archiver isn't slow enough to make this worthwhile)
 * - cache .a -> .so (see NOTE 3)
 * - determine unknown #include dependencies by calling clang ourself
 *
 * NOTE 1: We have the limitation that, in order to restore from build
 * cache, we need to know all of the #include file hashes. For CMake\ninja
 * this is done by parsing the .ninja_deps file created by ninja. This
 * file only exists if ninja has already built this C++ file one time.
 *
 * NOTE 2: We should cache ndk-build. At the time this was written, we
 * didn't have a perfgate test for ndk-build to validate the improvement.
 * We should add that too.
 *
 * NOTE 3: There are a few issues with caching .o/.a -> .so:
 * - Any change to any source file invalidates caching
 * - We'd have to support .o -> .a caching (to get the same .a as before)
 * - .so are very large and would make both load and store very heavy
 * Gradle's own C++ build cache also doesn't cache .so results
 */
class CxxBuildCache(
    private val buildCacheController: BuildCacheController,
    private val fileHasher: FileHasher) {
    private val hashFile = { file : File ->
        if (!file.isFile) {
            error("Could not hash '$file' because it didn't exist")
        }
        fileHasher.hash(file).toString()
    }

    /**
     * Enact C/C++ caching around [build] which is the ninja or ndk-build build
     * operation.
     * Cache for a particular [abi].
     * then load and store all .cpp to .o translations.
     */
    fun cacheBuild(abi : CxxAbiModel, build : () -> Unit) {
        if (!buildCacheController.isEnabled) {
            build()
            return
        }
        val cacheScope = SourceToObjectCacheScope(
            abi,
            buildCacheController,
            hashFile
        )
        cacheScope.restoreObjectFiles()
        try {
            build()
        } catch (e : Exception) {
            cacheScope.storeObjectFiles(buildFailed = true)
            throw e
        }
        cacheScope.storeObjectFiles(buildFailed = false)
    }
}



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

import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome
import com.google.common.hash.Hashing
import org.gradle.caching.BuildCacheKey
import java.io.File
import java.lang.Long.max

/**
 * Represents build cache information about a single .cpp to .o
 * compilation and provides related [BuildCacheKey]s.
 */
class BuildCacheKeys(
    private val clangKeySegment : String,
    val compilation: Compilation,
    val hashedCompilation: Compilation,
    val hasKnownDependencies: Boolean) {

    /**
     * Build cache key that represents all of the inputs to source to
     * object translation.
     */
    val objectKey : BuildCacheKey by lazy {
        createObjectKey()
    }

    /**
     * This is a key based only on the source and object file paths.
     * It's used only when the user passed:
     *
     *   -Dorg.gradle.caching.debug=true
     *
     * to track additional information to be able to explain a cache
     * miss.
     */
    val explanationKey : BuildCacheKey by lazy {
        createExplanationKey()
    }

    private fun createObjectKey() : BuildCacheKey {
        val key : ByteArray = hashedCompilation.objectFileKey.toByteArray()
        val keyHash = Hashing.murmur3_128(7).hashBytes(key).toString()
        val inputName = File(compilation.objectFileKey.dependencyKey.sourceFile).name
        val outputName = File(compilation.objectFile).name
        val displayName = "compile $inputName to $outputName"
        val hashCode = "cxx/$outputName/object/$clangKeySegment$keyHash"

        return object : BuildCacheKey {
            override fun getDisplayName() = displayName
            override fun getHashCode() = hashCode
            override fun toByteArray() = key
        }
    }


    private fun createExplanationKey() : BuildCacheKey {
        val key : ByteArray = compilation.objectFile.toByteArray()
        val keyHash = Hashing.murmur3_128(7).hashBytes(key).toString()
        val inputName = File(compilation.objectFileKey.dependencyKey.sourceFile).name
        val outputName = File(compilation.objectFile).name
        val displayName = "compile $inputName to $outputName (explanation)"
        val hashCode = "cxx/$outputName/explain/$clangKeySegment$keyHash"

        return object : BuildCacheKey {
            override fun getDisplayName() = displayName
            override fun getHashCode() = hashCode
            override fun toByteArray() = key
        }
    }

    /**
     * Path to the object file this key refers to.
     */
    val objectFile : File get() =
        File(compilation.workingDirectory).resolve(compilation.objectFile)

    /**
     * Return the max last-modified date for all dependencies.
     */
    val maxDependencyLastModified : Long get() = run {
        var max = File(compilation.objectFileKey.dependencyKey.sourceFile).lastModified()
        compilation.objectFileKey.dependenciesList.forEach { dependency ->
            max = max(max, File(dependency).lastModified())
        }
        max
    }

    /**
     * Create structured log record for this build cache event.
     */
    fun getObjectFileCacheEvent(outcome: Outcome): ObjectFileCacheEvent {
        return ObjectFileCacheEvent.newBuilder()
            .setOutcome(outcome)
            .setKeyDisplayName(objectKey.displayName)
            .setKeyHashCode(objectKey.hashCode)
            .setCompilation(compilation)
            .setHashedCompilation(hashedCompilation)
            .build()
    }
}

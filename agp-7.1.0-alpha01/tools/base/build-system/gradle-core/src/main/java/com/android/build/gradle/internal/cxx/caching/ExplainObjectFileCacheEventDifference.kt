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

import java.io.File
import kotlin.math.max

fun explainObjectFileCacheEventDifference(
    pastEvent: ObjectFileCacheEvent,
    currentEvent: ObjectFileCacheEvent
): String? {
    val reasons = mutableListOf<String>()
    reasons.addAll(explainFlagDifferences(
        pastEvent.compilation.objectFileKey.dependencyKey.compilerFlagsList,
        currentEvent.compilation.objectFileKey.dependencyKey.compilerFlagsList))
    reasons.addAll(explainDependencyDifferences(
        pastEvent.compilation.workingDirectory,
        pastEvent.compilation.objectFileKey.dependenciesList,
        pastEvent.hashedCompilation.objectFileKey.dependenciesList,
        currentEvent.compilation.workingDirectory,
        currentEvent.compilation.objectFileKey.dependenciesList,
        currentEvent.hashedCompilation.objectFileKey.dependenciesList,
    ))
    if (reasons.isEmpty()) return null
    if (reasons.size == 1) {
        return reasons[0]
    }
    if (reasons.size == 2) {
        return reasons[0] + " and 1 other reason"
    }
    return reasons[0] + " and ${reasons.size - 1} other reasons"
}

private fun relativeTo(fileName : String, folderName : String) : String {
    return File(fileName).relativeToOrSelf(File(folderName)).path
}

private fun explainDependencyDifferences(
    pastWorkingDir : String,
    pastIncludes: List<String>,
    pastIncludeHashes: List<String>,
    currentWorkingDir : String,
    currentIncludes: List<String>,
    currentIncludeHashes: List<String>): List<String> {
    val result = mutableListOf<String>()
    for(i in 0 until max(pastIncludes.size, currentIncludes.size)) {
        when {
            i >= pastIncludes.size ->
                result += "new dependency ${relativeTo(currentIncludes[i], currentWorkingDir)}"
            i >= currentIncludes.size ->
                result += "missing dependency ${relativeTo(pastIncludes[i], pastWorkingDir)}"
            pastIncludeHashes[i] != currentIncludeHashes[i] -> {
                result += "change in ${relativeTo(currentIncludes[i], currentWorkingDir)}"
            }
        }
    }
    return result
}

private fun explainFlagDifferences(
    pastFlags: List<String>,
    currentFlags: List<String>): List<String> {
    val result = mutableListOf<String>()
    for(i in 0 until max(pastFlags.size, currentFlags.size)) {
        when {
            i >= pastFlags.size ->
                result += "new flag '${currentFlags[i]}'"
            i >= currentFlags.size ->
                result += "missing flag '${pastFlags[i]}'"
            currentFlags[i] != pastFlags[i] ->
                result += "flag change from '${pastFlags[i]}' to '${currentFlags[i]}'"
        }
    }
    return result
}

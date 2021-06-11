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

import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.LOADED
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_LOADED_DEPENDENCIES_NOT_KNOWN
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_LOADED_KEY_DIDNT_EXIST
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_LOADED_SAME_OBJECT_FILE_LOCALLY
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_STORED_CACHE_ENTRY_ALREADY_EXISTED
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_STORED_COMPILER_DIDNT_PRODUCE_OBJECT_FILE
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_STORED_DEPENDENCIES_NOT_KNOWN
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_STORED_BUILD_FAILED
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.NOT_STORED_OBJECT_OLDER_THAN_DEPENDENCIES
import com.android.build.gradle.internal.cxx.caching.ObjectFileCacheEvent.Outcome.STORED
import com.android.build.gradle.internal.cxx.logging.infoln
import com.android.build.gradle.internal.cxx.logging.lifecycleln
import com.android.build.gradle.internal.cxx.logging.logStructured
import com.android.build.gradle.internal.cxx.model.CxxAbiModel
import com.android.build.gradle.internal.cxx.model.compileCommandsJsonBinFile
import com.android.build.gradle.internal.cxx.model.ifLogNativeBuildToLifecycle
import com.android.build.gradle.internal.cxx.timing.time
import com.android.utils.cxx.CompileCommand
import com.android.utils.cxx.CxxDiagnosticCode.OBJECT_FILE_NOT_RESTORED_ALREADY_EXISTED
import com.android.utils.cxx.CxxDiagnosticCode.OBJECT_FILE_NOT_RESTORED_CHANGED_INPUTS
import com.android.utils.cxx.CxxDiagnosticCode.OBJECT_FILE_NOT_RESTORED_NO_DEPENDENCIES
import com.android.utils.cxx.CxxDiagnosticCode.OBJECT_FILE_RESTORED
import com.android.utils.cxx.streamCompileCommandsV2
import org.gradle.caching.internal.controller.BuildCacheController
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Cache the translation of C/C++ source file to object file.
 *
 */
class SourceToObjectCacheScope constructor(
    private val streamCompileCommands : (CompileCommand.(List<String>?) -> Unit) -> Unit,
    private val logNativeBuildToLifecycle : Boolean,
    private val keyMaker : BuildCacheKeyMaker,
    private val buildCacheController: BuildCacheController) {

    constructor(
        abi : CxxAbiModel,
        buildCacheController: BuildCacheController,
        hashFile: (File) -> String) :
        this(
            getCompileCommandStreamer(abi),
            abi.ifLogNativeBuildToLifecycle { true } ?: false,
            BuildCacheKeyMaker(
                abi.variant.module.ndkVersion,
                abi.variant.module.ndkFolder,
                hashFile
            ),
            buildCacheController)

    /**
     * Restore .o files from build cache if they match current settings.
     */
    fun restoreObjectFiles() = time("restore-objects-from-build-cache") {
        streamCompileCommands { dependencies ->
            val keys = keyMaker.makeKeys(
                sourceFile,
                flags,
                workingDirectory,
                outputFile,
                dependencies)
            val outcome = loadObjectFileFromCache(keys)
            explainObjectFileLoadEvent(
                keys,
                outcome,
                sourceFileIndex,
                sourceFileCount,
                outputFile)
        }
    }

    /**
     * Store .o files to cache if they have changed.
     * When [buildFailed] is true, result objects are not stored but a structured log
     * outcome is still emitted. It will have outcome set to [NOT_STORED_BUILD_FAILED]
     */
    fun storeObjectFiles(buildFailed : Boolean) = time("store-objects-to-build-cache") {
        streamCompileCommands { dependencies ->
            val keys = keyMaker.makeKeys(
                sourceFile,
                flags,
                workingDirectory,
                outputFile,
                dependencies)
            val outcome = if (buildFailed) NOT_STORED_BUILD_FAILED
            else storeObjectFileToCache(keys)
            if (outcome == STORED) {
                infoln("[${sourceFileIndex + 1}/$sourceFileCount] Stored object $outputFile to Gradle build cache")
            }
            logStructured { encoder ->
                keys.getObjectFileCacheEvent(outcome).encode(encoder)
            }
        }
    }

    private fun loadObjectFileFromCache(keys : BuildCacheKeys) : ObjectFileCacheEvent.Outcome {
        if (!keys.hasKnownDependencies) return NOT_LOADED_DEPENDENCIES_NOT_KNOWN
        return buildCacheController.load(keys.objectKey) { input ->
            val data = DataInputStream(input)
            val objectFileHashCode = data.readUTF()

            if (keys.hashedCompilation.objectFile == objectFileHashCode) {
                NOT_LOADED_SAME_OBJECT_FILE_LOCALLY
            } else {
                keys.objectFile.parentFile.mkdirs()
                FileOutputStream(keys.objectFile).use { input.copyTo(it) }

                keys.objectFile.setLastModified(keys.maxDependencyLastModified)
                LOADED
            }
        } ?: NOT_LOADED_KEY_DIDNT_EXIST
    }

    private fun storeObjectFileToCache(keys : BuildCacheKeys) : ObjectFileCacheEvent.Outcome {
        if (!keys.hasKnownDependencies) return NOT_STORED_DEPENDENCIES_NOT_KNOWN
        if (!keys.objectFile.isFile) return NOT_STORED_COMPILER_DIDNT_PRODUCE_OBJECT_FILE
        if (keys.objectFile.lastModified() < keys.maxDependencyLastModified) return NOT_STORED_OBJECT_OLDER_THAN_DEPENDENCIES
        if (buildCacheController.load(keys.objectKey) { true } == true) return NOT_STORED_CACHE_ENTRY_ALREADY_EXISTED
        buildCacheController.store(keys.objectKey) { output ->
            val data = DataOutputStream(output)
            data.writeUTF(keys.hashedCompilation.objectFile)
            FileInputStream(keys.objectFile).use { it.copyTo(output) }
        }
        if (buildCacheController.isEmitDebugLogging) {
            // Store information to later explain a cache miss.
            buildCacheController.store(keys.explanationKey) { output ->
                keys.getObjectFileCacheEvent(STORED).writeDelimitedTo(output)
            }
        }
        return STORED
    }

    /**
     * Log information about why an object file was restored or not restored.
     */
    private fun explainObjectFileLoadEvent(
        keys: BuildCacheKeys,
        outcome : ObjectFileCacheEvent.Outcome,
        sourceFileIndex : Int,
        sourceFileCount : Int,
        objectFile : File) {

        // Only instantiate the event if it is needed by any of the logging logic.
        val loadEvent by lazy { keys.getObjectFileCacheEvent(outcome) }

        logStructured { encoder ->
            loadEvent.encode(encoder)
        }

        if (outcome == LOADED) {
            if (logNativeBuildToLifecycle || buildCacheController.isEmitDebugLogging) {
                lifecycleln(
                    OBJECT_FILE_RESTORED,
                    "[${sourceFileIndex + 1}/$sourceFileCount] Restored object $objectFile from Gradle build cache",)
            }
            return
        }

        if (!buildCacheController.isEmitDebugLogging) return
        when(outcome) {
            NOT_LOADED_KEY_DIDNT_EXIST -> {
                // Try to load the prior STORE corresponding to this object file
                val storeEvent = buildCacheController.load(keys.explanationKey) { input ->
                    ObjectFileCacheEvent.parseDelimitedFrom(input)
                }
                if (storeEvent == null) {
                    error("no prior store event")
                }
                else {
                    val difference = explainObjectFileCacheEventDifference(storeEvent, loadEvent)
                        ?: "unknown reasons"
                    lifecycleln(
                        OBJECT_FILE_NOT_RESTORED_CHANGED_INPUTS,
                        "[${sourceFileIndex + 1}/$sourceFileCount] Object $objectFile not restored from Gradle build cache because of $difference"
                    )
                }
            }
            NOT_LOADED_DEPENDENCIES_NOT_KNOWN ->
                lifecycleln(
                    OBJECT_FILE_NOT_RESTORED_NO_DEPENDENCIES,
                    "[${sourceFileIndex + 1}/$sourceFileCount] Object $objectFile not restored from Gradle build cache because it's #include dependencies could not be determined")
            NOT_LOADED_SAME_OBJECT_FILE_LOCALLY ->
                lifecycleln(
                    OBJECT_FILE_NOT_RESTORED_ALREADY_EXISTED,
                    "[${sourceFileIndex + 1}/$sourceFileCount] Object $objectFile already existed locally")
            else -> error("$outcome")
        }
    }

    companion object {

        /**
         * Creates a function to stream [CompileCommand]s for a particular [abi].
         * It computes the dependencies for the reference .o if they are available
         * and it skips compilations that are not used by the current target set
         * in build.gradle.
         */
        private fun getCompileCommandStreamer(abi : CxxAbiModel) : (action: CompileCommand.(List<String>?) -> Unit) -> Unit {
            return { action: CompileCommand.(List<String>?) -> Unit ->
                if (abi.compileCommandsJsonBinFile.exists()) {
                    val dependencyProvider = abi.createObjectFileDependencyProvider()
                    val targets = abi.variant.buildTargetSet
                    if (targets.isEmpty()) {
                        streamCompileCommandsV2(abi.compileCommandsJsonBinFile) {
                            action(this, dependencyProvider.dependencies(outputFile.path))
                        }
                    } else {
                        streamCompileCommandsV2(abi.compileCommandsJsonBinFile) {
                            if (targets.contains(target)) {
                                action(this, dependencyProvider.dependencies(outputFile.path))
                            }
                        }
                    }
                }
            }
        }
    }
}





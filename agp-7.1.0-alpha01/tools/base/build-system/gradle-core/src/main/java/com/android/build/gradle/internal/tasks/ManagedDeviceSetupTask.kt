/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.build.gradle.internal.tasks

import com.android.build.api.dsl.TestOptions
import com.android.build.gradle.internal.AvdComponentsBuildService
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.SdkComponentsBuildService
import com.android.build.gradle.internal.computeAvdName
import com.android.build.gradle.internal.dsl.ManagedVirtualDevice
import com.android.build.gradle.internal.profile.AnalyticsService
import com.android.build.gradle.internal.profile.ProfileAwareWorkAction
import com.android.build.gradle.internal.scope.GlobalScope
import com.android.build.gradle.internal.services.getBuildService
import com.android.build.gradle.internal.tasks.factory.GlobalTaskCreationAction
import com.android.build.gradle.internal.utils.setDisallowChanges
import com.android.repository.Revision
import com.android.testing.utils.computeSystemImageHashFromDsl
import com.android.utils.GrabProcessOutput
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.lang.Exception
import java.util.concurrent.TimeUnit

private const val SYSTEM_IMAGE_PREFIX = "system-images;"
private const val HASH_DIVIDER = ";"
private const val WAIT_AFTER_BOOT_MS = 5000L
private const val DEVICE_BOOT_TIMEOUT_SEC = 80L

private val loggerWrapper = LoggerWrapper.getLogger(ManagedDeviceSetupTask::class.java)

/**
 * Task to create an AVD from a managed device definition in the DSL.
 *
 * Expands the dsl from a [ManagedVirtualDevice] definition in [TestOptions.devices] to a functional
 * Android Virtual Device to be used with the emulator. This includes the downloading of required
 * system images, configuring the AVD, and creating an AVD snapshot.
 *
 * This task is required as a dependency for all Unified Testing Platform Tasks that require this
 * device.
 */
abstract class ManagedDeviceSetupTask: NonIncrementalGlobalTask() {

    @get: Internal
    abstract val sdkService: Property<SdkComponentsBuildService>

    @get: Internal
    abstract val avdService: Property<AvdComponentsBuildService>

    @get: Input
    abstract val compileSdkVersion: Property<String>

    @get: Input
    abstract val buildToolsRevision: Property<Revision>

    @get: Input
    abstract val abi: Property<String>

    @get: Input
    abstract val apiLevel: Property<Int>

    @get: Input
    abstract val systemImageVendor: Property<String>

    @get: Input
    abstract val hardwareProfile: Property<String>

    override fun doTaskAction() {
        workerExecutor.noIsolation().submit(ManagedDeviceSetupRunnable::class.java) {
            it.initializeWith(projectName,  path, analyticsService)
            it.sdkService.set(sdkService)
            it.compileSdkVersion.set(compileSdkVersion)
            it.buildToolsRevision.set(buildToolsRevision)
            it.avdService.set(avdService)
            it.imageHash.set(computeImageHash())
            it.deviceName.set(
                computeAvdName(
                    apiLevel.get(), systemImageVendor.get(), abi.get(), hardwareProfile.get()))
            it.hardwareProfile.set(hardwareProfile)
        }
    }

    abstract class ManagedDeviceSetupRunnable : ProfileAwareWorkAction<ManagedDeviceSetupParams>() {
        override fun run() {
            val versionedSdkLoader = parameters.sdkService.get().sdkLoader(
                compileSdkVersion = parameters.compileSdkVersion,
                buildToolsRevision = parameters.buildToolsRevision
            )
            parameters.avdService.get().avdProvider(
                versionedSdkLoader.sdkImageDirectoryProvider(parameters.imageHash.get()),
                parameters.imageHash.get(),
                parameters.deviceName.get(),
                parameters.hardwareProfile.get()).get()

            parameters.avdService.get().ensureLoadableSnapshot(
                parameters.deviceName.get())
        }
    }

    abstract class ManagedDeviceSetupParams : ProfileAwareWorkAction.Parameters() {
        abstract val sdkService: Property<SdkComponentsBuildService>
        abstract val compileSdkVersion: Property<String>
        abstract val buildToolsRevision: Property<Revision>
        abstract val avdService: Property<AvdComponentsBuildService>
        abstract val imageHash: Property<String>
        abstract val deviceName: Property<String>
        abstract val hardwareProfile: Property<String>
    }

    private fun computeImageHash(): String {
        return computeSystemImageHashFromDsl(apiLevel.get(), systemImageVendor.get(), abi.get())
    }

    class CreationAction(
        override val name: String,
        private val systemImageSource: String,
        private val apiLevel: Int,
        private val abi: String,
        private val hardwareProfile: String,
        globalScope: GlobalScope
    ) : GlobalTaskCreationAction<ManagedDeviceSetupTask>(globalScope) {

        constructor(
            name: String,
            managedDevice: ManagedVirtualDevice,
            globalScope: GlobalScope
        ): this(
            name,
            managedDevice.systemImageSource,
            managedDevice.apiLevel,
            managedDevice.abi,
            managedDevice.device,
            globalScope)

        override val type: Class<ManagedDeviceSetupTask>
            get() = ManagedDeviceSetupTask::class.java

        override fun configure(task: ManagedDeviceSetupTask) {
            task.sdkService.setDisallowChanges(globalScope.sdkComponents)
            task.compileSdkVersion.setDisallowChanges(globalScope.extension.compileSdkVersion)
            task.buildToolsRevision.setDisallowChanges(globalScope.extension.buildToolsRevision)
            task.avdService.setDisallowChanges(globalScope.avdComponents)

            task.systemImageVendor.setDisallowChanges(systemImageSource)
            task.apiLevel.setDisallowChanges(apiLevel)
            task.abi.setDisallowChanges(abi)
            task.hardwareProfile.setDisallowChanges(hardwareProfile)
            task.analyticsService.set(
                getBuildService(
                    task.project.gradle.sharedServices, AnalyticsService::class.java
                )
            )
        }

    }
}

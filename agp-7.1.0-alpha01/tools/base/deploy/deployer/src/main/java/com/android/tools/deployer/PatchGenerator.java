/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.deployer;

import com.android.tools.deployer.model.Apk;
import com.android.tools.deployer.model.ApkEntry;
import com.android.tools.tracer.Trace;
import com.android.utils.ILogger;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class PatchGenerator {

    public static class Patch {
        enum Status {
            Ok,
            SizeThresholdExceeded
        };

        final Status status;

        final ByteBuffer data;
        final ByteBuffer instructions;
        final String sourcePath; // Path to apk used as source of clean data on the device.
        final long destinationSize; // Size of apk to generate on the device.

        Patch(ByteBuffer data, ByteBuffer instructions, String sourcePath, long destinationSize) {
            this.data = data;
            this.instructions = instructions;
            this.sourcePath = sourcePath;
            this.destinationSize = destinationSize;
            this.status = Status.Ok;
        }

        Patch(Status status) {
            this.data = null;
            this.instructions = null;
            this.sourcePath = null;
            this.destinationSize = 0;
            this.status = status;
        }
    }

    private ILogger logger;

    public PatchGenerator(ILogger logger) {
        this.logger = logger;
    }

    /**
     * Generate the instructions and payload necessary to generate the localApk using a patch and
     * the remoteApk only.
     *
     * <p>Patch are generated by leveraging the Central Directory of an.apk files. By using only the
     * CD of each apks on the device (typically accounting for a few KiB), and then comparing CD
     * entries in the local apk on the host, a map of dirty areas is generated.
     *
     * @return A Patch to apply to a file in order to turn the remoteApk into the localApk.
     */
    public Patch generate(Apk remoteApk, Apk localApk) throws IOException {
        String sourcePath = remoteApk.path;
        long destinationSize = Files.size(Paths.get(localApk.path));

        // Generate maps from each apk, based on the content directory.
        List<ApkMap.Area> dirtyAreas = generateDirtyMap(remoteApk, localApk);

        // Use the map of what is dirty and what is clean in the archive to build the patching instruction.
        int patchSize = 0;
        for (ApkMap.Area dirtyArea : dirtyAreas) {
            patchSize += dirtyArea.size();
        }

        if (patchSize > PatchSetGenerator.MAX_PATCHSET_SIZE) {
            return new Patch(Patch.Status.SizeThresholdExceeded);
        }

        ByteBuffer data = ByteBuffer.wrap(new byte[patchSize]);
        ByteBuffer instructions =
                ByteBuffer.wrap(new byte[dirtyAreas.size() * 8]).order(ByteOrder.LITTLE_ENDIAN);

        Trace.begin("building patch");
        try (FileChannel fileChannel =
                FileChannel.open(Paths.get(localApk.path), StandardOpenOption.READ)) {
            for (ApkMap.Area dirtyArea : dirtyAreas) {
                instructions.putInt((int) dirtyArea.start);
                instructions.putInt((int) dirtyArea.size());
                data.limit((int) (data.position() + dirtyArea.size()));
                fileChannel.read(data, dirtyArea.start);
            }
        }
        Trace.end();

        data.rewind();
        instructions.rewind();
        return new Patch(data, instructions, sourcePath, destinationSize);
    }

    // Generate a patch for an apk which will result in a no-op when the patch is applied to it
    // (a patch with no data and no instruction but correct path and dst filesize).
    public Patch generateCleanPatch(Apk remoteApk, Apk localApk) throws IOException {
        String sourcePath = remoteApk.path;
        long destinationSize = Files.size(Paths.get(localApk.path));
        return new Patch(null, null, sourcePath, destinationSize);
    }

    private List<ApkMap.Area> generateDirtyMap(Apk remoteApk, Apk localApk) throws IOException {
        Trace.begin("marking dirty");
        ApkMap dirtyMap = new ApkMap(Files.size(Paths.get(localApk.path)));

        for (ApkEntry remoteEntry : remoteApk.apkEntries.values()) {
            ApkEntry localEntry = localApk.apkEntries.get(remoteEntry.getName());
            if (localEntry == null) {
                continue; // Skip Deleted file
            }
            if (!Arrays.equals(
                    remoteEntry.getZipEntry().localFileHeader,
                    localEntry.getZipEntry().localFileHeader)) {
                // This entry has changed and is considered dirty.
                continue;
            }

            // The entry has not changed. We can mark it a clean areas.
            ApkMap.Area cleanArea =
                    new ApkMap.Area(
                            localEntry.getZipEntry().start, localEntry.getZipEntry().approx_end);
            dirtyMap.markClean(cleanArea);
        }
        Trace.end();
        logger.info("Num dirty areas %d", dirtyMap.getDirtyAreas().size());
        return dirtyMap.getDirtyAreas();
    }
}

/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.integration.common.utils;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.integration.common.truth.ApkSubject;
import com.android.ide.common.process.CachedProcessOutputHandler;
import com.android.ide.common.process.DefaultProcessExecutor;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessExecutor;
import com.android.ide.common.process.ProcessInfo;
import com.android.utils.LineCollector;
import com.android.utils.StdLogger;
import com.google.common.base.Splitter;
import com.google.common.io.LineProcessor;
import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper to help read/test the content of generated apk file.
 */
public class ApkHelper {

    private static final Pattern PATTERN_LOCALES = Pattern.compile(
            "^locales\\W*:\\W*(.+)$");

    /**
     * Runs a process, and returns the output.
     *
     * @param processInfo the process info to run
     *
     * @return the output as a list of output lines.
     */
    @NonNull
    public static List<String> runAndGetOutput(@NonNull ProcessInfo processInfo) {

        ProcessExecutor executor = new DefaultProcessExecutor(
                new StdLogger(StdLogger.Level.ERROR));
        LineCollector lineCollector = new LineCollector();
        try {
            runAndProcessOutput(processInfo, executor, lineCollector);
        } catch (ProcessException e) {
            throw new RuntimeException(e);
        }
        return lineCollector.getResult();
    }

    /**
     * Runs a process, and tunnel the output to a {@link LineProcessor}.
     *
     * @param processInfo the process info to run
     * @param processExecutor the process executor
     * @param lineProcessor the processor to handle the process output line by line
     * @param <T> the expected result from the line processor
     * @return the result from the line processor
     */
    public static <T> T runAndProcessOutput(
            @NonNull ProcessInfo processInfo,
            @NonNull ProcessExecutor processExecutor,
            @NonNull LineProcessor<T> lineProcessor)
            throws ProcessException {

        CachedProcessOutputHandler handler = new CachedProcessOutputHandler();
        processExecutor.execute(processInfo, handler).rethrowFailure().assertNormalExitValue();
        return handler.getProcessOutput().processStandardOutputLines(lineProcessor);
    }

    /**
     * Returns the locales of an apk as found in the badging information
     * @param apk the apk
     * @return the list of locales or null.
     */
    @Nullable
    public static List<String> getLocales(@NonNull File apk) {
        List<String> output = ApkSubject.getBadging(apk.toPath());

        for (String line : output) {
            Matcher m = PATTERN_LOCALES.matcher(line.trim());
            if (m.matches()) {
                List<String> list = Splitter.on(' ').splitToList(m.group(1).trim());
                return list.stream()
                        // remove the '' on each side, if any present
                        .map(local -> local.replaceAll("^'", "").replaceAll("'$", ""))
                        .collect(Collectors.toList());
            }
        }

        return null;
    }
}

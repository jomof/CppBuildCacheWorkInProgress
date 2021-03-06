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
package com.android.testutils;

import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

public class DelegatingRunnerBuilder extends RunnerBuilder {

    private final RunnerBuilder defaultBuilder;
    private final IgnoredTestsRunnerBuilder ignoredTestsBuilder;

    public DelegatingRunnerBuilder(RunnerBuilder defaultBuilder) {
        this.defaultBuilder = defaultBuilder;
        this.ignoredTestsBuilder = new IgnoredTestsRunnerBuilder();
    }

    @Override
    public Runner runnerForClass(Class<?> testClass) throws Throwable {
        if (Boolean.getBoolean("ignored.tests.only")) {
            return ignoredTestsBuilder.runnerForClass(testClass);
        } else {
            return defaultBuilder.runnerForClass(testClass);
        }
    }
}

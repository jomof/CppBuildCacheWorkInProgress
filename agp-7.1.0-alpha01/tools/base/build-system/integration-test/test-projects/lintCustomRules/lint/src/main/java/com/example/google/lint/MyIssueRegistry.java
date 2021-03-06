/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.google.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.Vendor;
import com.android.tools.lint.detector.api.Issue;
import java.util.Collections;
import java.util.List;

/** The list of issues that will be checked when running <code>lint</code>. */
@SuppressWarnings("unused")
public class MyIssueRegistry extends IssueRegistry {
    private static final Vendor VENDOR = new Vendor("Google", "LintCustomRuleTest", null, null);

    @Override
    public Vendor getVendor() {
        return VENDOR;
    }

    @Override
    public List<Issue> getIssues() {
        return Collections.singletonList(MainActivityDetector.ISSUE);
    }

    @Override
    public int getApi() {
        return com.android.tools.lint.detector.api.ApiKt.CURRENT_API;
    }
}

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

package com.android.build.gradle.internal.dsl

import com.google.common.truth.Truth
import org.junit.Test

class LanguageSplitOptionsTest {

    @Test
    fun testDisabled() {
        val options = LanguageSplitOptions()
        options.include("en")

        val values = options.applicationFilters
        Truth.assertThat(values).isEmpty()
    }

    @Test
    fun testEnabled() {
        val options = LanguageSplitOptions()
        options.include("en")

        // Only when the filter is enabled, the included values should be returned.
        options.isEnable = true

        val values = options.applicationFilters
        Truth.assertThat(values).containsExactly("en")
    }
}
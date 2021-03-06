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

package com.android.build.api.dsl

/**
 * Multi variant publishing options.
 */
interface MultipleVariants {

    /**
     * Publish all the variants to the component.
     */
    fun allVariants()

    /**
     * Publish variants to the component based on the specified build types.
     */
    fun includeBuildTypeValues(vararg buildTypes: String)

    /**
     * Publish variants to the component based on the specified product flavor dimension and values.
     */
    fun includeFlavorDimensionAndValues(dimension: String, vararg values: String)
}

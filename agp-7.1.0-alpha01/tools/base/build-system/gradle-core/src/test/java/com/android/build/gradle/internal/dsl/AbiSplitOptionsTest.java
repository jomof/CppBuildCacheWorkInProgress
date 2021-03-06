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

package com.android.build.gradle.internal.dsl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.build.api.dsl.AbiSplit;
import com.android.build.gradle.internal.dsl.decorator.AndroidPluginDslDecoratorKt;
import com.android.build.gradle.internal.services.DslServices;
import com.android.build.gradle.internal.services.FakeServices;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import org.junit.Test;

public class AbiSplitOptionsTest {

    private interface AbiSplitOptionsWrapper {
        AbiSplit getAbiSplit();
    }

    private AbiSplitOptions getAbiSplitOptionsInstance() {
        DslServices dslServices = FakeServices.createDslServices();
        try {
            return (AbiSplitOptions)
                    AndroidPluginDslDecoratorKt.getAndroidPluginDslDecorator()
                            .decorate(AbiSplitOptionsWrapper.class)
                            .getDeclaredConstructor(DslServices.class)
                            .newInstance(dslServices)
                            .getAbiSplit();
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException e) {
            return null;
        }
    }

    @Test
    public void testDisabled() {
        AbiSplitOptions options = getAbiSplitOptionsInstance();

        Set<String> values = options.getApplicableFilters();

        assertEquals(0, values.size());
    }

    @Test
    public void testUnallowedInclude() {
        AbiSplitOptions options = getAbiSplitOptionsInstance();
        options.setEnable(true);

        String wrongValue = "x86_126bit";
        options.include(wrongValue);

        Set<String> values = options.getApplicableFilters();

        // test wrong value isn't there.
        assertFalse(values.contains(wrongValue));

        // test another default value shows up
        assertTrue(values.contains("x86"));
    }

    @Test
    public void testExclude() {
        AbiSplitOptions options = getAbiSplitOptionsInstance();
        options.setEnable(true);

        String oldValue = "armeabi";
        options.exclude(oldValue);

        Set<String> values = options.getApplicableFilters();

        assertFalse(values.contains(oldValue));
    }
}

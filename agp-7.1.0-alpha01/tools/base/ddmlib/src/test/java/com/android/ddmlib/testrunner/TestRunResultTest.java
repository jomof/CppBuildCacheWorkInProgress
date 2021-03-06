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
package com.android.ddmlib.testrunner;

import static org.junit.Assert.*;

import com.android.ddmlib.testrunner.TestResult.TestStatus;
import java.util.Collections;
import org.junit.Test;

/** Unit tests for {@link TestRunResult} */
public class TestRunResultTest {

    @Test
    public void testGetNumTestsInState() {
        TestIdentifier test = new TestIdentifier("FooTest", "testBar");
        TestRunResult result = new TestRunResult();
        assertEquals(0, result.getNumTestsInState(TestStatus.PASSED));
        result.testStarted(test);
        assertEquals(0, result.getNumTestsInState(TestStatus.PASSED));
        assertEquals(1, result.getNumTestsInState(TestStatus.INCOMPLETE));
        result.testEnded(test, Collections.EMPTY_MAP);
        assertEquals(1, result.getNumTestsInState(TestStatus.PASSED));
        assertEquals(0, result.getNumTestsInState(TestStatus.INCOMPLETE));
    }

    /** Test that we are able to specify directly the start and end time of a test. */
    @Test
    public void testSpecifyElapsedTime() {
        TestIdentifier test = new TestIdentifier("FooTest", "testBar");
        TestRunResult result = new TestRunResult();
        result.testStarted(test, 5l);
        assertEquals(5l, result.getTestResults().get(test).getStartTime());
        result.testEnded(test, 25l, Collections.EMPTY_MAP);
        assertEquals(25l, result.getTestResults().get(test).getEndTime());
    }
}

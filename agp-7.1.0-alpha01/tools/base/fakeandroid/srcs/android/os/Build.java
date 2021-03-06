/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.os;

/** A skeleton class to act as a test mock */
public class Build {
    /** Various version strings. */
    public static class VERSION {
        public static final int SDK_INT = 0;
    }
    public static class VERSION_CODES {
        public static final int M = 23;
    }

    // it returns unexistend "host" ABI on purpose, because
    // .so libraries of correct architecture still won't work on host
    // thus we use unexistent "host" abi and place use corresponding
    // .so libraries
    public static String[] SUPPORTED_ABIS = new String[] {"host"};
}

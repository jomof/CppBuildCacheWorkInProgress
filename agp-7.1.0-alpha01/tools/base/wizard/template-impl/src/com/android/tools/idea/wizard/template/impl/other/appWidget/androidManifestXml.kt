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

package com.android.tools.idea.wizard.template.impl.other.appWidget

import com.android.tools.idea.wizard.template.renderIf

fun androidManifestXml(
  className: String,
  configurable: Boolean,
  layoutName: String,
  packageName: String
) = """
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application>

        <receiver
            android:name="${packageName}.${className}"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>

            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/${layoutName}_info" />
        </receiver>

${renderIf(configurable) {"""
        <activity android:name="${packageName}.${className}ConfigureActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
            </intent-filter>
        </activity>
"""}}
    </application>

</manifest>
"""

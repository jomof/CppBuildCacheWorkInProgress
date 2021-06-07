/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.tools.idea.wizard.template.impl.activities.blankWearActivity

import com.android.tools.idea.wizard.template.activityToLayout
import com.android.tools.idea.wizard.template.renderIf

fun androidManifestXml(
  activityClass: String,
  isLauncher: Boolean,
  isLibrary: Boolean,
  isNewModule: Boolean,
  packageName: String
): String {
  val labelBlock = if (isNewModule) "android:label=\"@string/app_name\""
  else "android:label=\"@string/title_${activityToLayout(activityClass)}\""

  val intentFilterBlock = renderIf((isLauncher || isNewModule) && !isLibrary) {"""
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
  """
  }
  return """
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:name="android.hardware.type.watch" />

    <application>

        <uses-library android:name="com.google.android.wearable" android:required="true" />

        <!--
               Set to true if your app is Standalone, that is, it does not require the handheld
               app to run.
        -->
        <meta-data android:name="com.google.android.wearable.standalone" android:value="true"/>

        <activity android:name="${packageName}.${activityClass}"
            android:exported="true"
            $labelBlock
            >
            $intentFilterBlock
        </activity>
    </application>

</manifest>
"""
}
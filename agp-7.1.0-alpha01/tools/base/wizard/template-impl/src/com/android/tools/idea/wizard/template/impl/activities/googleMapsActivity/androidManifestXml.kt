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

package com.android.tools.idea.wizard.template.impl.activities.googleMapsActivity

import com.android.tools.idea.wizard.template.impl.activities.common.commonActivityBody
import com.android.tools.idea.wizard.template.impl.activities.common.collapseEmptyActivityTags

fun androidManifestXml(
  activityClass: String,
  isLauncher: Boolean,
  isLibrary: Boolean,
  packageName: String,
  simpleName: String,
  isNewModule: Boolean
): String {
  // TODO(qumeric): add activityLabel like in other activity templates
  val activityBody = commonActivityBody(isLauncher || isNewModule, isLibrary)

  return """
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
      <!--
         The ACCESS_COARSE/FINE_LOCATION permissions are not required to use
         Google Maps Android API v2, but you must specify either coarse or fine
         location permissions for the "MyLocation" functionality.
      -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

    <application>

        <!--
             The API key for Google Maps-based APIs is defined as a string resource.
             (See the file "res/values/google_maps_api.xml").
             Note that the API key is linked to the encryption key used to sign the APK.
             You need a different API key for each encryption key, including the release key that is used to
             sign the APK for publishing.
             You can define the keys for the debug and release targets in src/debug/ and src/release/.
         -->
        <meta-data android:name="com.google.android.geo.API_KEY" android:value="@string/google_maps_key"/>

        <activity android:name="${packageName}.${activityClass}"
            android:exported="true"
            android:label="@string/title_${simpleName}">
            $activityBody
        </activity>
    </application>

</manifest>
""".collapseEmptyActivityTags()
}

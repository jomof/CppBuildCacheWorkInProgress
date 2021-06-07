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

package com.android.tools.idea.wizard.template.impl.fragments.settingsFragment.res.xml

fun rootPreferencesXml() = """
<PreferenceScreen
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <PreferenceCategory
        app:title="@string/messages_header">

        <EditTextPreference
            app:key="signature"
            app:title="@string/signature_title"
            app:useSimpleSummaryProvider="true"/>

        <ListPreference
            app:key="reply"
            app:title="@string/reply_title"
            app:entries="@array/reply_entries"
            app:entryValues="@array/reply_values"
            app:defaultValue="reply"
            app:useSimpleSummaryProvider="true"/>

    </PreferenceCategory>

    <PreferenceCategory
        app:title="@string/sync_header">

        <SwitchPreferenceCompat
            app:key="sync"
            app:title="@string/sync_title"/>

        <SwitchPreferenceCompat
            app:key="attachment"
            app:title="@string/attachment_title"
            app:summaryOn="@string/attachment_summary_on"
            app:summaryOff="@string/attachment_summary_off"
            app:dependency="sync"/>

    </PreferenceCategory>

</PreferenceScreen>
"""
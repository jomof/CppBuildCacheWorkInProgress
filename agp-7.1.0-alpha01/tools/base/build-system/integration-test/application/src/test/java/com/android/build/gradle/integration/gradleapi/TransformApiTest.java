/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.build.gradle.integration.gradleapi;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.android.SdkConstants;
import com.android.build.gradle.integration.common.fixture.BaseGradleExecutor;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.utils.ProjectBuildOutputUtils;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.VariantBuildInformation;
import com.android.testutils.apk.Apk;
import com.google.common.io.Files;
import java.io.File;
import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test for building a transform against version 1.5. */
public class TransformApiTest {

    @Rule
    public GradleTestProject wholeProject = GradleTestProject.builder()
            .fromTestProject("transformApiTest")
            .create();

    @Before
    public void moveLocalProperties() throws Exception {
        // Only one of the projects is an Android project, and there is no top-level
        // settings.gradle, so local.properties ends up not being picked up. Just move
        // it to the project that needs it.
        Files.move(
                wholeProject.file(SdkConstants.FN_LOCAL_PROPERTIES),
                wholeProject
                        .getSubproject("androidproject")
                        .file(SdkConstants.FN_LOCAL_PROPERTIES));
    }

    @Test
    public void checkRepackagedGsonLibrary() throws Exception {
        wholeProject
                .getSubproject("plugin")
                .executor()
                // uploadArchives is not compatible
                .withConfigurationCaching(BaseGradleExecutor.ConfigurationCaching.OFF)
                .withFailOnWarning(false)
                .run("publish");

        wholeProject
                .getSubproject("androidproject")
                .executor()
                .withFailOnWarning(false)
                .run("assembleDebug");

        // get the output model
        AndroidProject projectModel =
                wholeProject
                        .getSubproject("androidproject")
                        .model()
                        .withFailOnWarning(false)
                        .fetchAndroidProjects()
                        .getOnlyModel();
        VariantBuildInformation debugVariantOutput =
                ProjectBuildOutputUtils.getDebugVariantBuildOutput(projectModel);
        assertNotNull("debug Variant null-check", debugVariantOutput);

        // get the outputs.
        Collection<String> debugOutputs =
                ProjectBuildOutputUtils.getOutputFiles(debugVariantOutput);
        assertNotNull(debugOutputs);
        assertEquals(1, debugOutputs.size());

        // make sure the Gson library has been renamed and the original one is not present.
        Apk outputFile =
                new Apk(new File(ProjectBuildOutputUtils.getSingleOutputFile(debugVariantOutput)));
        assertThatApk(outputFile).containsClass("Lcom/google/repacked/gson/Gson;");
        assertThatApk(outputFile).doesNotContainClass("Lcom/google/gson/Gson;");
    }
}

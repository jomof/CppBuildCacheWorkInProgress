/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.build.gradle.integration.application;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;
import static org.junit.Assert.assertTrue;

import com.android.build.gradle.integration.common.fixture.GradleTaskExecutor;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.fixture.TestVersions;
import com.android.build.gradle.integration.common.fixture.app.EmptyAndroidTestApp;
import com.android.build.gradle.integration.common.fixture.app.HelloWorldApp;
import com.android.build.gradle.integration.common.fixture.app.MultiModuleTestProject;
import com.android.build.gradle.integration.common.runner.FilterableParameterized;
import com.android.build.gradle.integration.common.utils.TestFileUtils;
import com.android.build.gradle.options.BooleanOption;
import com.android.ide.common.process.ProcessException;
import com.android.testutils.apk.Apk;
import com.android.utils.FileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Test desugaring using D8. */
@RunWith(FilterableParameterized.class)
public class D8DesugaringTest {

    @Parameterized.Parameters(name = "withDexingArtifactTransform_{0}")
    public static List<Object> parameters() {
        return ImmutableList.of(true, false);
    }

    private final boolean withDexingArtifactTransform;
    @Rule
    public GradleTestProject project;

    public D8DesugaringTest(boolean withDexingArtifactTransform) {
        this.withDexingArtifactTransform = withDexingArtifactTransform;
        project = GradleTestProject.builder()
                .fromTestApp(
                        new MultiModuleTestProject(
                                ImmutableMap.of(
                                        ":app",
                                        HelloWorldApp.noBuildFile(),
                                        ":lib",
                                        new EmptyAndroidTestApp())))
                .addGradleProperties(
                        BooleanOption.ENABLE_DEXING_ARTIFACT_TRANSFORM.getPropertyName()
                                + "="
                                + withDexingArtifactTransform)
                .create();
    }

    @Before
    public void setUp() throws IOException {
        TestFileUtils.appendToFile(
                project.getSubproject(":app").getBuildFile(),
                "\n"
                        + "apply plugin: \"com.android.application\"\n"
                        + "\n"
                        + "apply from: \"../../commonLocalRepo.gradle\"\n"
                        + "\n"
                        + "android {\n"
                        + "    compileSdkVersion "
                        + GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
                        + "\n"
                        + "\n"
                        + "    defaultConfig {\n"
                        + "        applicationId \"com.example.d8desugartest\"\n"
                        + "        minSdkVersion 20\n"
                        + "        testInstrumentationRunner "
                        + "\"android.support.test.runner.AndroidJUnitRunner\"\n"
                        + "    }\n"
                        + "\n"
                        + "  flavorDimensions \"whatever\"\n"
                        + "\n"
                        + "    productFlavors {\n"
                        + "      multidex {\n"
                        + "        dimension \"whatever\"\n"
                        + "        multiDexEnabled true\n"
                        + "        multiDexKeepProguard file('debug_main_dex_rules.txt')\n"
                        + "      }\n"
                        + "      base {\n"
                        + "        dimension \"whatever\"\n"
                        + "      }\n"
                        + "    }\n"
                        + "    compileOptions {\n"
                        + "        sourceCompatibility JavaVersion.VERSION_1_8\n"
                        + "        targetCompatibility JavaVersion.VERSION_1_8\n"
                        + "    }\n"
                        + "\n"
                        + "    dependencies {\n"
                        + "        implementation project(':lib')\n"
                        + "        androidTestImplementation"
                        + " 'com.android.support:support-v4:"
                        + TestVersions.SUPPORT_LIB_VERSION
                        + "'\n"
                        + "        testImplementation 'junit:junit:4.12'\n"
                        + "        androidTestImplementation 'com.android.support.test:runner:"
                        + TestVersions.TEST_SUPPORT_LIB_VERSION
                        + "'\n"
                        + "        androidTestImplementation 'com.android.support.test:rules:"
                        + TestVersions.TEST_SUPPORT_LIB_VERSION
                        + "'\n"
                        + "    }\n"
                        + "}\n");

        TestFileUtils.appendToFile(
                project.getSubproject(":lib").getBuildFile(), "apply plugin: 'java'\n");
        File interfaceWithDefault =
                project.getSubproject(":lib")
                        .file("src/main/java/com/example/helloworld/InterfaceWithDefault.java");
        FileUtils.mkdirs(interfaceWithDefault.getParentFile());
        TestFileUtils.appendToFile(
                interfaceWithDefault,
                "package com.example.helloworld;\n"
                        + "\n"
                        + "public interface InterfaceWithDefault {\n"
                        + "  \n"
                        + "  static String defaultConvert(String input) {\n"
                        + "    return input + \"-default\";\n"
                        + "  }\n"
                        + "  \n"
                        + "  default String convert(String input) {\n"
                        + "    return defaultConvert(input);\n"
                        + "  }\n"
                        + "}\n");
        File stringTool =
                project.getSubproject(":lib")
                        .file("src/main/java/com/example/helloworld/StringTool.java");
        FileUtils.mkdirs(stringTool.getParentFile());
        TestFileUtils.appendToFile(
                stringTool,
                "package com.example.helloworld;\n"
                        + "\n"
                        + "public class StringTool {\n"
                        + "  private InterfaceWithDefault converter;\n"
                        + "  public StringTool() {\n"
                        + "    this(new InterfaceWithDefault() { });\n"
                        + "  }\n"
                        + "  public StringTool(InterfaceWithDefault converter) {\n"
                        + "    this.converter = converter;\n"
                        + "  }\n"
                        + "  public String convert(String input) {\n"
                        + "    return converter.convert(input);\n"
                        + "  }\n"
                        + "}\n");
        File exampleInstrumentedTest =
                project.getSubproject(":app")
                        .file(
                                "src/androidTest/java/com/example/helloworld/ExampleInstrumentedTest.java");
        FileUtils.mkdirs(exampleInstrumentedTest.getParentFile());
        TestFileUtils.appendToFile(
                exampleInstrumentedTest,
                "package com.example.helloworld;\n"
                        + "\n"
                        + "import android.content.Context;\n"
                        + "import android.support.test.InstrumentationRegistry;\n"
                        + "import android.support.test.runner.AndroidJUnit4;\n"
                        + "\n"
                        + "import org.junit.Test;\n"
                        + "import org.junit.runner.RunWith;\n"
                        + "\n"
                        + "import static org.junit.Assert.*;\n"
                        + "\n"
                        + "@RunWith(AndroidJUnit4.class)\n"
                        + "public class ExampleInstrumentedTest {\n"
                        + "    @Test\n"
                        + "    public void useAppContext() throws Exception {\n"
                        + "        // Context of the app under test.\n"
                        + "        Context appContext ="
                        + " InstrumentationRegistry.getTargetContext();\n"
                        + "        assertEquals(\"toto-default\", "
                        + "new StringTool().convert(\"toto\"));\n"
                        + "    }\n"
                        + "}\n");
        File debugMainDexRules = project.getSubproject(":app").file("debug_main_dex_rules.txt");
        TestFileUtils.appendToFile(
                debugMainDexRules, "-keep class com.example.helloworld.StringTool { *; }");
    }

    @Test
    public void checkDesugaring() throws IOException, InterruptedException {
        GradleTaskExecutor executor = project.executor();
        executor.run("assembleBaseDebug", "assembleBaseDebugAndroidTest");
        Apk androidTestApk =
                project.getSubproject(":app")
                        .getApk(GradleTestProject.ApkType.ANDROIDTEST_DEBUG, "base");
        assertThat(androidTestApk).hasClass("Lcom/example/helloworld/ExampleInstrumentedTest;");
        assertThat(androidTestApk).hasDexVersion(35);
        Apk androidApk =
                project.getSubproject(":app").getApk(GradleTestProject.ApkType.DEBUG, "base");
        assertThat(androidApk).hasClass("Lcom/example/helloworld/InterfaceWithDefault;");
        assertThat(androidApk).hasDexVersion(35);
    }

    @Test
    public void checkMinified() throws IOException, InterruptedException {
        // Regression test for http://b/72624896
        TestFileUtils.appendToFile(
                project.getSubproject(":app").getBuildFile(),
                "android.buildTypes.debug.minifyEnabled true");
        TestFileUtils.addMethod(
                FileUtils.join(
                        project.getSubproject(":app").getMainSrcDir(),
                        "com/example/helloworld/HelloWorld.java"),
                "Runnable r = () -> {};");
        project.executor().run("assembleBaseDebug");
        Apk androidApk =
                project.getSubproject(":app").getApk(GradleTestProject.ApkType.DEBUG, "base");
        assertThat(androidApk).hasDexVersion(35);
    }

    @Test
    public void checkMultidex() throws IOException, InterruptedException, ProcessException {
        GradleTaskExecutor executor = project.executor();
        executor.run("assembleMultidexDebug");
        Apk multidexApk =
                project.getSubproject(":app").getApk(GradleTestProject.ApkType.DEBUG, "multidex");
        assertThat(multidexApk).hasMainClass("Lcom/example/helloworld/InterfaceWithDefault;");
        boolean foundTheSynthetic = false;
        assertTrue(multidexApk.getMainDexFile().isPresent());
        for (String clazz : multidexApk.getMainDexFile().get().getClasses().keySet()) {
            if (clazz.contains("-CC")) {
                foundTheSynthetic = true;
                break;
            }
        }
        assertThat(foundTheSynthetic).isTrue();
    }
}

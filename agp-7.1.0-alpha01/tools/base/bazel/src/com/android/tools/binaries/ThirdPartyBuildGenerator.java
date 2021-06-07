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

package com.android.tools.binaries;

import static com.google.common.base.Preconditions.checkNotNull;

import com.android.tools.maven.AetherUtils;
import com.android.tools.maven.HighestVersionSelector;
import com.android.tools.maven.MavenCoordinates;
import com.android.tools.maven.MavenRepository;
import com.android.tools.utils.Buildifier;
import com.android.tools.utils.WorkspaceUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;

/**
 * Binary that generates a BUILD file (most likely in //tools/base/third_party) which mimics the
 * Maven dependency graph using java_libraries with exports.
 */
public class ThirdPartyBuildGenerator {
    private static final String PREBUILTS_BAZEL_PACKAGE = "//prebuilts/tools/common/m2/repository/";
    private static final String GENERATED_WARNING =
            "#\n"
                    + "# !!! ATTENTION !!!\n"
                    + "#\n"
                    + "# This BUILD file was generated by //tools/base/bazel:third_party_build_generator.\n"
                    + "# Please do not edit manually, as your changes will be overwritten by the next dependency update.\n"
                    + "#\n"
                    + "# See //tools/base/bazel/README.md for details."
                    + "\n";

    /**
     * Dependencies excluded from the resolution process.
     *
     * <p>We don't use asm-all (that some libraries depend on), instead we use the individual
     * packages.
     */
    static final ImmutableList<Exclusion> EXCLUSIONS =
            ImmutableList.of(
                    new Exclusion("com.google.guava", "guava-jdk5", "*", "*"),
                    new Exclusion("org.ow2.asm", "asm-all", "*", "*"),
                    new Exclusion("org.ow2.asm", "asm-debug-all", "*", "*"));

    public static void main(String[] argsArray) throws Exception {
        List<String> args = Lists.newArrayList(argsArray);
        Path buildFile;
        Path localRepo;

        if (!args.isEmpty() && !MavenCoordinates.isMavenCoordinate(args.get(0))) {
            buildFile = Paths.get(args.get(0));
            localRepo = Paths.get(args.get(1));
            args.remove(0);
            args.remove(0);
        } else {
            Path workspace = WorkspaceUtils.findWorkspace();
            buildFile = workspace.resolve("tools/base/third_party/BUILD");
            localRepo = WorkspaceUtils.findPrebuiltsRepository();
        }

        if (!Files.isDirectory(localRepo) || !Files.isRegularFile(buildFile)) {
            usage();
        }

        Stream<String> artifacts;
        if (!args.isEmpty()) {
            artifacts = args.stream();
        } else {
            Path dependenciesPropertiesFile =
                    WorkspaceUtils.findWorkspace()
                            .resolve("tools/buildSrc/base/dependencies.properties");
            artifacts = readDependenciesProperties(dependenciesPropertiesFile);
        }
        Set<Artifact> artifactSet = artifacts.map(DefaultArtifact::new).collect(Collectors.toSet());

        new ThirdPartyBuildGenerator(buildFile, localRepo).generateBuildFile(artifactSet);
    }

    private static void usage() {
        System.out.println(
                "Usage: third_party_build_generator [path/to/BUILD path/to/m2/repository] com.example:foo:1.0 ...");
        System.out.println("");
        System.err.println(
                "If the paths to m2 repo and BUILD are omitted, the ones from current WORKSPACE "
                        + "will be used.");
        System.exit(1);
    }

    private final Path mBuildFile;
    private final MavenRepository mRepo;

    @VisibleForTesting
    protected ThirdPartyBuildGenerator(Path buildFile, Path localRepo) {
        mBuildFile = checkNotNull(buildFile);
        mRepo = new MavenRepository(localRepo);
    }

    private void generateBuildFile(Set<Artifact> artifacts)
            throws DependencyCollectionException, IOException, ArtifactDescriptorException,
                    DependencyResolutionException {
        SortedMap<String, Artifact> versions = computeEffectiveVersions(artifacts);

        Files.createDirectories(mBuildFile.getParent());
        if (Files.exists(mBuildFile)) {
            Files.delete(mBuildFile);
        }

        try (FileWriter fileWriter = new FileWriter(mBuildFile.toFile())) {
            fileWriter.append(GENERATED_WARNING);
            fileWriter.append(System.lineSeparator());
            fileWriter.append("load(\"//tools/base/bazel:maven.bzl\", \"maven_java_library\")");
            fileWriter.append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());
            fileWriter.append("# Used by test");
            fileWriter.append(System.lineSeparator());
            fileWriter.append(
                    "exports_files(['BUILD'], visibility=[\"//tools/base/bazel:__pkg__\"])\n");
            fileWriter.append(System.lineSeparator());
            fileWriter.append(System.lineSeparator());

            for (Map.Entry<String, Artifact> entry : versions.entrySet()) {
                String ruleName = entry.getKey();
                Artifact artifact = entry.getValue();
                List<String> deps =
                        getDirectDependencies(artifact, JavaScopes.COMPILE, versions.keySet());
                List<String> runtimeDeps =
                        getDirectDependencies(artifact, JavaScopes.RUNTIME, versions.keySet());

                fileWriter.append(
                        "# ATTENTION: This file is generated from "
                                + "tools/buildSrc/base/dependencies.properties, "
                                + "see top of this file.\n");
                fileWriter.append("maven_java_library(");
                fileWriter.append(String.format("name = \"%s\", ", ruleName));
                switch (artifact.getExtension()) {
                    case "jar":
                        fileWriter.append(
                                String.format("export_artifact = \"%s\", ", getTarget(artifact)));
                        break;
                    case "pom":
                        fileWriter.append(String.format("pom = \"%s\", ", getTarget(artifact)));
                        break;
                    default:
                        throw new IllegalStateException(
                                String.format(
                                        "Artifact '%s' has unknown packaging type '%s'.",
                                        artifact.toString(), artifact.getExtension()));
                }
                fileWriter.append("visibility = [\"//visibility:public\"], ");
                fileWriter.append(String.format("exports = [%s], ", formatAsBazelList(deps)));

                if (!runtimeDeps.isEmpty()) {
                    fileWriter.append(
                            String.format("runtime_deps = [%s], ", formatAsBazelList(runtimeDeps)));
                }

                fileWriter.append(")\n");
            }
        }
        Buildifier.runBuildifier(mBuildFile);
    }

    private static String formatAsBazelList(List<String> deps) {
        return deps.stream().map(s -> '"' + s + '"').collect(Collectors.joining(", "));
    }

    private List<String> getDirectDependencies(
            Artifact artifact, String scope, Set<String> allArtifacts)
            throws ArtifactDescriptorException {
        ArtifactDescriptorResult descriptor = mRepo.readArtifactDescriptor(artifact);

        return descriptor
                .getDependencies()
                .stream()
                .filter(dependency -> scope.equals(dependency.getScope()))
                .filter(dependency -> !dependency.isOptional())
                // This can be false, if a library was excluded using Maven exclusions.
                .filter(dependency -> allArtifacts.contains(getRuleName(dependency.getArtifact())))
                .map(dependency -> ":" + getRuleName(dependency.getArtifact()))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    protected static Stream<String> readDependenciesProperties(Path dependenciesPropertiesFile)
            throws IOException {
        Properties dependencies = new Properties();
        try (InputStream inputStream = Files.newInputStream(dependenciesPropertiesFile)) {
            dependencies.load(inputStream);
        }
        Stream<String> deps = dependencies.stringPropertyNames().stream();
        return deps.map(
                (key) -> {
                    String value = dependencies.getProperty(key);
                    // Hack to support pom packaging (bug 183921630)
                    if (value.startsWith("org.codehaus.groovy:groovy-all:3")) {
                        value =
                                "org.codehaus.groovy:groovy-all:pom:"
                                        + value.substring(
                                                "org.codehaus.groovy:groovy-all:".length());
                    }
                    return value;
                });
    }

    @VisibleForTesting
    protected String getTarget(Artifact artifact) {
        Path jar = mRepo.getRelativePath(artifact);
        String ruleName;
        switch (artifact.getExtension()) {
            case "jar":
                ruleName = JavaImportGenerator.JAR_RULE_NAME;
                break;
            case "pom":
                ruleName = JavaImportGenerator.POM_RULE_NAME;
                break;
            default:
                throw new IllegalStateException(
                        String.format(
                                "Artifact '%s' has unknown packaging type '%s'.",
                                artifact.toString(), artifact.getExtension()));
        }
        return PREBUILTS_BAZEL_PACKAGE + jar.getParent() + ":" + ruleName;
    }

    private SortedMap<String, Artifact> computeEffectiveVersions(Set<Artifact> artifacts)
            throws DependencyCollectionException, DependencyResolutionException, IOException {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setDependencies(
                artifacts.stream()
                        .map(artifact -> new Dependency(artifact, JavaScopes.COMPILE))
                        .collect(Collectors.toList()));

        collectRequest.setRepositories(AetherUtils.REPOSITORIES);

        DefaultRepositorySystemSession session = mRepo.getRepositorySystemSession();

        session.setDependencyGraphTransformer(
                new ConflictResolver(
                        new HighestVersionSelector(artifacts),
                        new JavaScopeSelector(),
                        new SimpleOptionalitySelector(),
                        new JavaScopeDeriver()));
        session.setDependencySelector(AetherUtils.buildDependencySelector(EXCLUSIONS));

        DependencyResult result = mRepo.resolveDependencies(new DependencyRequest(collectRequest, null));

        SortedMap<String, Artifact> versions = new TreeMap<>();

        JavaImportGenerator imports = new JavaImportGenerator(mRepo);
        result.getRoot()
                .accept(
                        new DependencyVisitor() {
                            @Override
                            public boolean visitEnter(DependencyNode node) {
                                Artifact artifact = node.getArtifact();
                                if (artifact != null) {

                                    versions.put(getRuleName(artifact), artifact);
                                    try {
                                        imports.generateImportRules(artifact);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                }

                                return true;
                            }

                            @Override
                            public boolean visitLeave(DependencyNode node) {
                                return true;
                            }
                        });
        return versions;
    }

    private String getRuleName(Artifact artifact) {
        return artifact.getGroupId() + "_" + artifact.getArtifactId();
    }
}
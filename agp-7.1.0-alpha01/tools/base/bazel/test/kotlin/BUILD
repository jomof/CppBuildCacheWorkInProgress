load("//tools/base/bazel:kotlin.bzl", "kotlin_library", "kotlin_test")
load("//tools/base/bazel:maven.bzl", "maven_java_library", "maven_pom", "maven_repo")

# Test just kotlin
java_library(
    name = "test_only_java",
    srcs = ["java0/pkg/J0.java"],
)

kotlin_library(
    name = "test_only_kotlin",
    srcs = ["kotlin0/pkg/K0.kt"],
    deps = [":test_only_java"],
)

# Test circular dependencies with kotlin and java
kotlin_library(
    name = "test_kotlin_and_java",
    srcs = [
        "java1_kotlin1/pkg/J1.java",
        "java1_kotlin1/pkg/K1.kt",
    ],
    deps = [":test_only_java"],
)

java_library(
    name = "test_java_to_kotlin",
    srcs = ["java2/pkg/J2.java"],
    deps = [
        ":test_kotlin_and_java",
        ":test_only_kotlin",
    ],
)

### Maven version:

# Test just kotlin
maven_java_library(
    name = "test_maven_only_java",
    srcs = ["java0/pkg/J0.java"],
    pom = ":test_maven_only_java_pom",
)

maven_pom(
    name = "test_maven_only_java_pom",
    artifact = "test_maven_only_java",
    group = "test_group",
    version = "1.2.3",
)

kotlin_library(
    name = "test_maven_only_kotlin",
    srcs = ["kotlin0/pkg/K0.kt"],
    pom = ":test_maven_only_kotlin_pom",
    deps = [":test_maven_only_java"],
)

maven_pom(
    name = "test_maven_only_kotlin_pom",
    artifact = "test_maven_only_kotlin",
    group = "test_group",
    version = "1.2.3",
)

# Test circular dependencies with kotlin and java
kotlin_library(
    name = "test_maven_kotlin_and_java",
    srcs = [
        "java1_kotlin1/pkg/J1.java",
        "java1_kotlin1/pkg/K1.kt",
    ],
    pom = ":test_maven_kotlin_and_java_pom",
    deps = [":test_maven_only_java"],
)

maven_pom(
    name = "test_maven_kotlin_and_java_pom",
    artifact = "test_maven_kotlin_and_java",
    group = "test_group",
    version = "1.2.3",
)

maven_java_library(
    name = "test_maven_java_to_kotlin",
    srcs = ["java2/pkg/J2.java"],
    pom = ":test_maven_java_to_kotlin_pom",
    deps = [
        ":test_maven_kotlin_and_java",
        ":test_maven_only_kotlin",
    ],
)

maven_pom(
    name = "test_maven_java_to_kotlin_pom",
    artifact = "test_maven_java_to_kotlin",
    group = "test_group",
    version = "1.2.3",
)

maven_repo(
    name = "test_repo",
    artifacts = [
        ":test_maven_java_to_kotlin",
    ],
)

maven_repo(
    name = "test_repo_from_kotlin",
    artifacts = [
        ":test_maven_only_kotlin",
    ],
)

# Tests

kotlin_test(
    name = "only_java_tests",
    srcs = ["java0_test/pkg/J0Test.java"],
    test_class = "pkg.J0Test",
    deps = [
        ":test_only_java",
        "//tools/base/third_party:junit_junit",
    ],
)

kotlin_test(
    name = "only_kotlin_tests",
    srcs = ["kotlin0_test/pkg/K0Test.kt"],
    test_class = "pkg.K0Test",
    deps = [
        ":test_only_kotlin",
        "//tools/base/third_party:junit_junit",
    ],
)

kotlin_test(
    name = "java_kotlin_tests",
    srcs = [
        "java1_kotlin1_test/pkg/J1Test.java",
        "java1_kotlin1_test/pkg/K1Test.kt",
    ],
    test_class = "pkg.K1Test",
    deps = [
        ":test_kotlin_and_java",
        ":test_only_java",
        ":test_only_kotlin",
        "//tools/base/third_party:junit_junit",
    ],
)

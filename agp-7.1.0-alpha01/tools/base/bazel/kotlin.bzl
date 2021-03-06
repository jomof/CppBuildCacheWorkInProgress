load(":coverage.bzl", "coverage_baseline", "coverage_java_test")
load(":functions.bzl", "explicit_target")
load(":maven.bzl", "maven_pom")
load(":merge_archives.bzl", "merge_jars")
load(":lint.bzl", "lint_test")

def test_kotlin_use_ir():
    return select({
        "//tools/base/bazel:kotlin_no_use_ir": False,
        "//conditions:default": True,
    })

def kotlin_compile(ctx, name, srcs, deps, friends, out, jre):
    """Runs kotlinc on the given source files.

    Args:
        ctx: the analysis context
        name: the name of the module being compiled
        srcs: a list of Java and Kotlin source files
        deps: a list of JavaInfo providers from direct dependencies
        friends: a list of friend jars (allowing access to 'internal' members)
        out: the output jar file
        jre: list of jars from the JRE bootclasspath

    Returns:
        JavaInfo for the resulting jar.

    Expects that ctx.files._kotlinc is defined.

    Note: kotlinc only compiles Kotlin, not Java. So if there are Java
    sources, then you will also need to run javac after this action.
    """

    # Normally ABI jars are used at compile time, but the ABI jars generated by
    # ijar are incorrect for Kotlin because (for example) kotlinc expects to see
    # the bodies of inline functions in class files. So, we have to explicitly
    # put 'full_compile_jars' on the classpath instead.
    #
    # TODO: eventually it should be possible to generate proper ABI jars from
    # Kotlin sources. There is already a Kotlin compiler plugin for it
    # (see https://youtrack.jetbrains.com/issue/KT-25128), and there is
    # experimental support for it in the 'official' Bazel Kotlin rules
    # (see https://github.com/bazelbuild/rules_kotlin/pull/294).
    #
    # TODO: even if we cannot create ABI jars for Kotlin, it would still be
    # beneficial to have kotlinc consume ABI jars from Java-only dependencies.
    # Consuming hjars should be safe because hjars are produced from Java
    # sources, never from Kotlin. Consuming ijars is trickier because ijars come
    # from prebuilt jars and we may not know the original source language.
    dep_jars = depset(transitive = [
        java_common.make_non_strict(dep).full_compile_jars
        for dep in deps
    ])

    args = ctx.actions.args()

    args.add("-module-name", name)
    args.add("-nowarn")  # Mirrors the default javac opts.
    args.add("-jvm-target", "1.8")
    args.add("-api-version", "1.3")  # b/166582569
    args.add("-Xjvm-default=enable")
    args.add("-no-stdlib")

    # Dependency jars may be compiled with a new kotlinc IR backend.
    args.add("-Xallow-unstable-dependencies")

    # Add "use-ir" to enable the new IR backend for kotlinc tasks when the
    # attribute "kotlin_use_ir" is set
    if ctx.attr.kotlin_use_ir:
        args.add("-Xuse-ir")

    # Use custom JRE instead of the default one picked by kotlinc.
    args.add("-no-jdk")
    classpath = depset(direct = jre, transitive = [dep_jars])

    # Note: there are some open questions regarding the transitivity of friends.
    # See https://github.com/bazelbuild/rules_kotlin/issues/211.
    args.add_joined(friends, join_with = ",", format_joined = "-Xfriend-paths=%s")

    args.add_joined("-cp", classpath, join_with = ":")
    args.add("-o", out)
    args.add_all(srcs)

    # To enable persistent Bazel workers, all arguments must come in an argfile.
    args.use_param_file("@%s", use_always = True)
    args.set_param_file_format("multiline")

    ctx.actions.run(
        inputs = depset(direct = srcs, transitive = [classpath]),
        outputs = [out],
        mnemonic = "kotlinc",
        arguments = [args],
        executable = ctx.executable._kotlinc,
        execution_requirements = {"supports-workers": "1"},
    )

    return JavaInfo(output_jar = out, compile_jar = out)

def _kotlin_jar_impl(ctx):
    deps = [dep[JavaInfo] for dep in ctx.attr.deps]
    deps.append(ctx.attr._kotlin_stdlib[JavaInfo])
    return kotlin_compile(
        ctx = ctx,
        name = ctx.attr.module_name,
        srcs = ctx.files.srcs,
        deps = deps,
        friends = ctx.files.friends,
        out = ctx.outputs.output_jar,
        jre = ctx.files._bootclasspath,
    )

_kotlin_jar = rule(
    attrs = {
        "srcs": attr.label_list(
            allow_empty = False,
            allow_files = True,
        ),
        "friends": attr.label_list(
            allow_files = [".jar"],
        ),
        "deps": attr.label_list(
            providers = [JavaInfo],
        ),
        "module_name": attr.string(
            default = "unnamed",
        ),
        "kotlin_use_ir": attr.bool(),
        "_bootclasspath": attr.label(
            # Use JDK 8 because AGP still needs to support it (b/166472930).
            default = Label("//prebuilts/studio/jdk:bootclasspath"),
            allow_files = [".jar"],
        ),
        "_kotlinc": attr.label(
            executable = True,
            cfg = "host",
            default = Label("//tools/base/bazel:kotlinc"),
            allow_files = True,
        ),
        "_kotlin_stdlib": attr.label(
            default = Label("//prebuilts/tools/common/kotlin-plugin-ij:Kotlin/kotlinc/lib/kotlin-stdlib"),
            allow_files = True,
        ),
    },
    outputs = {
        "output_jar": "lib%{name}.jar",
    },
    implementation = _kotlin_jar_impl,
)

def kotlin_library(
        name,
        srcs,
        javacopts = [],
        resources = [],
        resource_strip_prefix = None,
        deps = [],
        runtime_deps = [],
        bundled_deps = [],
        friends = [],
        data = [],
        pom = None,
        exclusions = None,
        visibility = None,
        jar_name = None,
        testonly = False,
        lint_baseline = None,
        lint_classpath = [],
        lint_is_test_sources = False,
        lint_timeout = None,
        module_name = None):
    """Compiles a library jar from Java and Kotlin sources"""
    kotlins = [src for src in srcs if src.endswith(".kt")]
    javas = [src for src in srcs if src.endswith(".java")]

    if not testonly:
        coverage_baseline(
            name = name,
            srcs = javas + kotlins,
        )

    if not kotlins and not javas:
        fail("No sources found for kotlin_library " + name)

    targets = []
    kdeps = []
    if kotlins:
        kotlin_name = name + ".kotlin"
        targets += [kotlin_name]
        kdeps += [kotlin_name]
        _kotlin_jar(
            name = kotlin_name,
            srcs = srcs,
            deps = deps + bundled_deps,
            friends = friends,
            visibility = visibility,
            testonly = testonly,
            module_name = module_name,
            kotlin_use_ir = test_kotlin_use_ir(),
        )

    java_name = name + ".java"
    resources_with_notice = native.glob(["NOTICE", "LICENSE"]) + resources if pom else resources
    final_javacopts = javacopts + ["--release", "8"]

    if javas or resources_with_notice:
        targets += [java_name]
        native.java_library(
            name = java_name,
            srcs = javas,
            javacopts = final_javacopts if javas else None,
            resources = resources_with_notice,
            resource_strip_prefix = resource_strip_prefix,
            deps = (kdeps + deps + bundled_deps) if javas else None,
            runtime_deps = runtime_deps,
            resource_jars = bundled_deps,
            visibility = visibility,
            testonly = testonly,
        )

    jar_name = jar_name if jar_name else "lib" + name + ".jar"
    merge_jars(
        name = name + ".singlejar",
        jars = [":lib" + target + ".jar" for target in targets],
        out = jar_name,
        allow_duplicates = True,  # TODO: Ideally we could be more strict here.
    )

    native.java_import(
        name = name,
        jars = [jar_name],
        deps = deps + ["//prebuilts/tools/common/kotlin-plugin-ij:Kotlin/kotlinc/lib/kotlin-stdlib"],
        data = data,
        visibility = visibility,
        testonly = testonly,
    )

    if pom:
        maven_pom(
            name = name + "_maven",
            deps = [explicit_target(dep) + "_maven" for dep in deps if not dep.endswith("_neverlink")],
            exclusions = exclusions,
            library = name,
            visibility = visibility,
            source = pom,
        )

    lint_srcs = javas + kotlins
    if lint_baseline:
        if not lint_srcs:
            fail("lint_baseline set for iml_module that has no sources")

        kwargs = {}
        if lint_timeout:
            kwargs["timeout"] = lint_timeout

        lint_test(
            name = name + "_lint_test",
            srcs = lint_srcs,
            baseline = lint_baseline,
            deps = deps + bundled_deps + lint_classpath,
            custom_rules = ["//tools/base/lint:studio-checks.lint-rules.jar"],
            tags = ["no_windows"],
            is_test_sources = lint_is_test_sources,
            **kwargs
        )

def kotlin_test(
        name,
        srcs,
        deps = [],
        runtime_deps = [],
        friends = [],
        visibility = None,
        lint_baseline = None,
        lint_classpath = [],
        **kwargs):
    kotlin_library(
        name = name + ".testlib",
        srcs = srcs,
        deps = deps,
        testonly = True,
        runtime_deps = runtime_deps,
        jar_name = name + ".jar",
        lint_baseline = lint_baseline,
        lint_classpath = lint_classpath,
        lint_is_test_sources = True,
        visibility = visibility,
        friends = friends,
    )

    coverage_java_test(
        name = name + ".test",
        runtime_deps = [
            ":" + name + ".testlib",
        ] + runtime_deps,
        visibility = visibility,
        **kwargs
    )

    native.test_suite(
        name = name,
        tests = [name + ".test"],
    )

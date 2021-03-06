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

package com.android.tools.lint.checks

import com.android.tools.lint.detector.api.Detector

class CheckResultDetectorTest : AbstractCheckTest() {
    override fun getDetector(): Detector = CheckResultDetector()

    fun testDocumentationExample() {
        lint().files(
            kotlin(
                """
                package test.pkg

                import androidx.annotation.CheckResult
                import java.math.BigDecimal

                @CheckResult
                fun BigDecimal.double() = this + this

                fun test(score: BigDecimal): BigDecimal {
                    score.double()
                    return score
                }
                """
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR
        ).run().expect(
            """
            src/test/pkg/test.kt:10: Warning: The result of double is not used [CheckResult]
                score.double()
                ~~~~~~~~~~~~~~
            0 errors, 1 warnings
            """
        )
    }

    fun testCheckResult() {
        val expected =
            """
            src/test/pkg/CheckPermissions.java:22: Warning: The result of extractAlpha is not used [CheckResult]
                    bitmap.extractAlpha(); // WARNING
                    ~~~~~~~~~~~~~~~~~~~~~
            src/test/pkg/Intersect.java:7: Warning: The result of intersect is not used. If the rectangles do not intersect, no change is made and the original rectangle is not modified. These methods return false to indicate that this has happened. [CheckResult]
                rect.intersect(aLeft, aTop, aRight, aBottom);
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            src/test/pkg/CheckPermissions.java:10: Warning: The result of checkCallingOrSelfPermission is not used; did you mean to call #enforceCallingOrSelfPermission(String,String)? [UseCheckPermission]
                    context.checkCallingOrSelfPermission(Manifest.permission.INTERNET); // WRONG
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            src/test/pkg/CheckPermissions.java:11: Warning: The result of checkPermission is not used; did you mean to call #enforcePermission(String,int,int,String)? [UseCheckPermission]
                    context.checkPermission(Manifest.permission.INTERNET, 1, 1);
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            0 errors, 4 warnings
            """
        lint().files(
            java(
                """
                package test.pkg;
                import android.Manifest;
                import android.content.Context;
                import android.content.pm.PackageManager;
                import android.graphics.Bitmap;

                @SuppressWarnings("ClassNameDiffersFromFileName")
                public class CheckPermissions {
                    private void foo(Context context) {
                        context.checkCallingOrSelfPermission(Manifest.permission.INTERNET); // WRONG
                        context.checkPermission(Manifest.permission.INTERNET, 1, 1);
                        check(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET)); // OK
                        int check = context.checkCallingOrSelfPermission(Manifest.permission.INTERNET); // OK
                        if (context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) // OK
                                != PackageManager.PERMISSION_GRANTED) {
                            showAlert(context, "Error",
                                    "Application requires permission to access the Internet");
                        }
                    }

                    private Bitmap checkResult(Bitmap bitmap) {
                        bitmap.extractAlpha(); // WARNING
                        Bitmap bitmap2 = bitmap.extractAlpha(); // OK
                        call(bitmap.extractAlpha()); // OK
                        return bitmap.extractAlpha(); // OK
                    }

                    private void showAlert(Context context, String error, String s) {
                    }

                    private void check(int i) {
                    }
                    private void call(Bitmap bitmap) {
                    }
                }"""
            ).indented(),

            java(
                "src/test/pkg/Intersect.java",
                """
                package test.pkg;
                import android.graphics.Rect;

                @SuppressWarnings({"ClassNameDiffersFromFileName", "MethodMayBeStatic"})
                public class Intersect {
                  void check(Rect rect, int aLeft, int aTop, int aRight, int aBottom) {
                    rect.intersect(aLeft, aTop, aRight, aBottom);
                  }
                }"""
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expect(expected)
    }

    fun testSubtract() {
        // Regression test for https://issuetracker.google.com/69344103:
        // @CanIgnoreReturnValue should let you *undo* a @CheckReturnValue on a class/package
        lint().files(
            java(
                """
                    package test.pkg;
                    import com.google.errorprone.annotations.CanIgnoreReturnValue;
                    import javax.annotation.CheckReturnValue;

                    @SuppressWarnings({"ClassNameDiffersFromFileName", "MethodMayBeStatic", "ResultOfMethodCallIgnored"})
                    @CheckReturnValue
                    public class IgnoreTest {
                        public String method1() {
                            return "";
                        }

                        public void method2() {
                        }

                        @CanIgnoreReturnValue
                        public String method3() {
                            return "";
                        }

                        public void test() {
                            method1(); // ERROR: should check
                            method2(); // OK: void return value
                            method3(); // OK: Specifically allowed
                        }
                    }
                """
            ).indented(),
            java(
                """
                    package com.google.errorprone.annotations;
                    import java.lang.annotation.Retention;
                    import static java.lang.annotation.RetentionPolicy.CLASS;
                    @SuppressWarnings("ClassNameDiffersFromFileName")
                    @Retention(CLASS)
                    public @interface CanIgnoreReturnValue {}"""
            ).indented(),
            javaxCheckReturnValueSource,
            SUPPORT_ANNOTATIONS_JAR
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expect(
                """
                src/test/pkg/IgnoreTest.java:21: Warning: The result of method1 is not used [CheckResult]
                        method1(); // ERROR: should check
                        ~~~~~~~~~
                0 errors, 1 warnings
                """
            )
    }

    fun testSubtract2() {
        // Regression test for https://issuetracker.google.com/69344103
        // Make sure we don't inherit @CheckReturn value from packages
        lint().files(
            java(
                """
                    package test.pkg;

                    @SuppressWarnings({"ClassNameDiffersFromFileName", "MethodMayBeStatic"})
                    public class IgnoreTest {
                        public String method() {
                            return "";
                        }

                        public void test() {
                            method(); // OK: not inheriting from packages
                        }
                    }
                """
            ).indented(),

            // Include the compiled version of the package-info file;
            // without this we can't resolve package annotations.
            compiled(
                "libs/packageinfoclass.jar",
                java(
                    "" +
                        "@CheckReturnValue\n" +
                        "package test.pkg;\n" +
                        "import javax.annotation.CheckReturnValue;\n"
                ),
                0xad536a8e,
                "test/pkg/package-info.class:" +
                    "H4sIAAAAAAAAAE1NvQrCMBi81J+qkw66ODprRgcnEQRBECq4p+Gzpo1JaZPi" +
                    "szn4AD6U2CqIN9zB/XDP1/0BYIl+iDBEj2FwtL6QtFWaGEa5kJlIaK7M2S5S" +
                    "UQmGaeSNU1famUqVKta0NsY64ZQ1JcNs37RuXPxMvrmQzCJyvjAnoT2tGMaO" +
                    "SsfzLOH/BwzDZsy1MAk/xClJNwEYAnwRoPXhNjq1duukC7wBEsYF4sIAAAA="
            ),
            javaxCheckReturnValueSource,
            SUPPORT_ANNOTATIONS_JAR
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expectClean()
    }

    fun testCheckResultInTests() {
        // Make sure tests work with checkTestSources true
        // Regression test for b/148841320
        lint().files(
            kotlin(
                "src/test/java/test/pkg/UnitTest.kt",
                """
                    package test.pkg
                    import androidx.annotation.CheckResult
                    fun something(list: List<String>) {
                        fromNullable(list)
                    }
                    @CheckResult
                    fun fromNullable(a: Any?): Any? = a
                    """
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR,
            gradle(
                """
                    android {
                        lintOptions {
                            checkTestSources true
                        }
                    }"""
            ).indented()
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expect(
                """
                src/test/java/test/pkg/UnitTest.kt:4: Warning: The result of fromNullable is not used [CheckResult]
                    fromNullable(list)
                    ~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
            )
    }

    fun testNotIgnoredInBlock() {
        // Regression test for
        // 69534608: False positive for "The result of <method_name> is not used"
        lint().files(
            kotlin(
                """
                    package test.pkg

                    import androidx.annotation.CheckResult

                    fun something(list: List<String>) {
                        list.map { fromNullable(it) }
                    }

                    @CheckResult
                    fun fromNullable(a: Any?): Any? = a"""
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expectClean()
    }

    fun testNotIgnoredInBlock2() {
        // Regression test for
        // 69534608: False positive for "The result of <method_name> is not used"
        lint().files(
            kotlin(
                """
                    package test.pkg

                    import androidx.annotation.CheckResult
                    fun test() {
                        val list = listOf(1, 2, 3)

                        val x1 = list.map {
                            label(it)
                        }
                        val x2 = list.map {
                            SomeClass.label(it)
                        }
                        val x3 = list.map {
                            SomeClass.create().label(it)
                        }
                        val x4: List<Any?> = list.map {
                            assert(it < 5)
                            SomeClass.label(it)
                        }
                    }

                    class SomeClass {
                        @CheckResult
                        fun label(a: Any): String = "value: ???a"

                        companion object {
                            @CheckResult
                            fun label(a: Any): String = "value: ???a"

                            fun create(): SomeClass {
                                return SomeClass()
                            }
                        }
                    }

                    @CheckResult
                    fun label(a: Any?): Any? = a"""
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expectClean()
    }

    fun testCheckResultIf() {
        // Regression test for
        // 72258872: Lint is wrongly detecting "CheckResult" in Kotlin code
        lint().files(
            kotlin(
                """
                    package test.pkg

                    import androidx.annotation.CheckResult

                    fun testIsUnused(): Int {
                        if (3 > 2) {
                            foo() // Unused
                        } else {
                            1
                        }
                        return 0
                    }

                    fun testReturn(): Int {
                        return if (3 > 2) {
                            foo() // OK
                        } else {
                            1
                        }
                    }

                    fun testExpressionBodyReturn(): Int =
                        return if (3 > 2) {
                            foo() // OK
                        } else {
                            1
                        }

                    fun testAssignment(): Int {
                        val result = if (3 > 2) {
                            foo() // OK
                        } else {
                            1
                        }
                        return result
                    }

                    fun testNesting(): Int {
                        return if (3 > 2) {
                            if (4 > 2) {
                                foo() // OK
                            } else {
                                1
                            }
                        } else {
                            1
                        }
                    }

                    @CheckResult
                    fun foo(): Int {
                        return 42
                    }
                """
            ),
            SUPPORT_ANNOTATIONS_JAR
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expect(
                """
                src/test/pkg/test.kt:8: Warning: The result of foo is not used [CheckResult]
                                            foo() // Unused
                                            ~~~~~
                0 errors, 1 warnings
                """
            )
    }

    fun test73563032() {
        // Regression test for
        //   https://issuetracker.google.com/73563032
        //   73563032: Lint is detecting a "CheckResult" issue when using lambdas in Kotlin
        lint().files(
            kotlin(
                """
                @file:Suppress("unused", "RemoveExplicitTypeArguments", "UNUSED_PARAMETER", "ConstantConditionIf")

                package test.pkg

                import androidx.annotation.CheckResult

                fun lambda1(): () -> Single<Int> = {
                    if (true) {
                        Single.just(3)
                    } else {
                        Single.just(5)
                    }
                }

                fun lambda2(): () -> Single<Int> = {
                    if (true)
                        Single.just(3)
                    else
                        Single.just(5)
                }

                class Single<T> {
                    companion object {
                        @CheckResult
                        fun just(int: Int): Single<Int> {
                            return Single<Int>()
                        }
                    }
                }
                """
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expectClean()
    }

    fun testChainedCalls() {
        lint().files(
            java(
                """
                package test.pkg;

                import androidx.annotation.CheckResult;

                @SuppressWarnings({"WeakerAccess", "ClassNameDiffersFromFileName"})
                public class CheckResultTest {
                    public void test() {
                        myMethod(); // WARN
                        this.myMethod(); // WARN
                        myMethod().print(); // OK
                    }

                    @CheckResult
                    public MyClass myMethod() {
                        return new MyClass();
                    }

                    class MyClass {
                        void print() {
                            System.out.println("World");
                        }
                    }
                }
                """
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expect(
                """
                src/test/pkg/CheckResultTest.java:8: Warning: The result of myMethod is not used [CheckResult]
                        myMethod(); // WARN
                        ~~~~~~~~~~
                src/test/pkg/CheckResultTest.java:9: Warning: The result of myMethod is not used [CheckResult]
                        this.myMethod(); // WARN
                        ~~~~~~~~~~~~~~~
                0 errors, 2 warnings
                """
            )
    }

    fun test80234958() {
        // 80234958: Lint check misses CheckResult inside kotlin class init blocks
        lint().files(
            kotlin(
                """
                package com.example

                import io.reactivex.Observable

                @Suppress("ConvertSecondaryConstructorToPrimary","RemoveExplicitTypeArguments")
                class Foo {
                  private val someObservable = Observable.create<Int> { }

                  init {
                    someObservable.subscribe { }
                  }

                  constructor() {
                    someObservable.subscribe { }
                  }

                  fun method() {
                    someObservable.subscribe { }
                  }
                }
                """
            ).indented(),
            java(
                """
                    // Stub
                    package io.reactivex;

                    import androidx.annotation.CheckResult;
                    import java.util.function.Consumer;

                    @SuppressWarnings("ClassNameDiffersFromFileName")
                    public abstract class Observable<T> {
                        @CheckResult
                        public static <T> Observable<T> create(Object source) {
                            return null;
                        }

                        @CheckResult
                        public final Object subscribe(Consumer<? super T> onNext) {
                            return null;
                        }
                    }

                """
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR
        )
            .issues(CheckResultDetector.CHECK_RESULT, CheckResultDetector.CHECK_PERMISSION)
            .run()
            .expect(
                """
                src/com/example/Foo.kt:10: Warning: The result of subscribe is not used [CheckResult]
                    someObservable.subscribe { }
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/Foo.kt:14: Warning: The result of subscribe is not used [CheckResult]
                    someObservable.subscribe { }
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/Foo.kt:18: Warning: The result of subscribe is not used [CheckResult]
                    someObservable.subscribe { }
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 3 warnings
                """
            )
    }

    fun test112602230() {
        // Regression test for
        // 112602230: Spurious lint error for unused result from AndroidFluentLogger#log
        lint().files(
            java(
                """
                package test.pkg;

                import com.google.errorprone.annotations.CheckReturnValue;

                @CheckReturnValue
                @SuppressWarnings({"ClassNameDiffersFromFileName", "MethodMayBeStatic"})
                public interface LoggingApi {
                    void log(String msg, Object p1);
                }
                """
            ),
            java(
                """
                package test.pkg;

                @SuppressWarnings({"ClassNameDiffersFromFileName", "MethodMayBeStatic"})
                public class LoggingApiTest {
                    public void test(LoggingApi api) {
                        api.log("log", null);
                    }
                }
                """
            ),
            kotlin(
                """
                package test.pkg

                import com.google.errorprone.annotations.CheckReturnValue;

                @Suppress("RedundantUnitReturnType")
                @CheckReturnValue
                interface LoggingApiKotlin {
                    fun log(msg: String, p1: Any): Unit
                }

                """
            ),
            kotlin(
                """
                package test.pkg

                class LoggingApiTestKotlin {
                    fun test(api: LoggingApiKotlin) {
                        api.log("log", "")
                    }
                }
                """
            ),
            errorProneCheckReturnValueSource
        ).run().expectClean()
    }

    fun test119270148() {
        // Regression test for 119270148
        lint().files(
            java(
                """
                package test.pkg;

                public class Test {
                    public Completable clear(KeyValueStore keyValueStore) {
                        return Completable.create(subscriber -> {
                            keyValueStore.clear();
                        });
                    }
                }
                """
            ),
            java(
                """
                package test.pkg;

                public interface Other {
                    void something(Object o);
                }
                """
            ),
            java(
                """
                package test.pkg;

                public class Completable {
                    public static Completable create(Other o) {
                        return null;
                    }
                }
                """
            ),
            kotlin(
                """
                package test.pkg

                import androidx.annotation.CheckResult

                interface Clearable {
                    @CheckResult
                    fun clear(): Completable
                }

                interface KeyValueStore : Clearable {
                    override fun clear(): Completable
                }
                """
            ),
            SUPPORT_ANNOTATIONS_JAR
        ).run().expect(
            """
            src/test/pkg/Test.java:7: Warning: The result of clear is not used [CheckResult]
                                        keyValueStore.clear();
                                        ~~~~~~~~~~~~~~~~~~~~~
            0 errors, 1 warnings
            """
        )
    }

    fun testIgnoreThisAndSuper() {
        // Regression test for b/140616532: Lint was flagging this() and super() constructor
        // calls
        lint().files(
            java(
                """
                package test.pkg;

                import com.google.errorprone.annotations.CheckReturnValue;

                @CheckReturnValue
                public class CheckResultTest1 {
                    CheckResultTest1() {
                        this(null);
                    }

                    CheckResultTest1(String foo) {
                    }

                    public class SubClass extends CheckResultTest1 {
                        SubClass(String foo) {
                            super(null);
                        }
                    }
                }
                """
            ),
            kotlin(
                """
                package test.pkg

                import com.google.errorprone.annotations.CheckReturnValue

                @CheckReturnValue
                open class CheckResultTest2 @JvmOverloads internal constructor(foo: String? = null) {
                    constructor(s: String, s2: String) : this(s)
                    inner class SubClass internal constructor(foo: String?) : CheckResultTest2(null)
                }
                """
            ),
            errorProneCheckReturnValueSource
        ).run().expectClean()
    }

    fun testCheckResultInLambda() {
        // 188436943: False negative in CheckResultDetector in Kotlin lambdas
        lint().files(
            kotlin(
                """
                import androidx.annotation.CheckResult
                import kotlin.random.Random

                @CheckResult
                fun checkReturn(): String {
                    return Random.nextInt().toString()
                }

                val unitLambda: () -> Unit = {
                    // Should flag: we know based on the lambda's type the return value is unused
                    checkReturn()
                }

                val valueLambda: () -> String = {
                    // Should flag: is not the last expression in the lambda, so value is unused
                    checkReturn()
                    "foo"
                }

                // 188855906: @CheckResult doesn't work inside lambda expressions

                @CheckResult fun x(): String = "Hello"

                fun y() {
                    x() // Correctly flagged

                    val x1 = run {
                        x() // Not flagged
                        ""
                    }

                    val x2 = "".also {
                        x() // Not flagged (the `also` block returns Unit)
                    }
                }
                """
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR
        ).run().expect(
            """
            src/test.kt:11: Warning: The result of checkReturn is not used [CheckResult]
                checkReturn()
                ~~~~~~~~~~~~~
            src/test.kt:16: Warning: The result of checkReturn is not used [CheckResult]
                checkReturn()
                ~~~~~~~~~~~~~
            src/test.kt:25: Warning: The result of x is not used [CheckResult]
                x() // Correctly flagged
                ~~~
            src/test.kt:28: Warning: The result of x is not used [CheckResult]
                    x() // Not flagged
                    ~~~
            src/test.kt:33: Warning: The result of x is not used [CheckResult]
                    x() // Not flagged (the `also` block returns Unit)
                    ~~~
            0 errors, 5 warnings
            """
        )
    }

    fun testBrackets() {
        // Regression test for b/189970773
        lint().files(
            kotlin(
                """
                @file:Suppress(
                    "ConstantConditionIf", "ControlFlowWithEmptyBody",
                    "IMPLICIT_CAST_TO_ANY", "IntroduceWhenSubject"
                )

                package test.pkg

                fun test() {
                    if (true) checkResult()     // ERROR 1
                    if (true) { checkResult() } // ERROR 2
                    if (true) { } else { checkResult() } // ERROR 3
                    if (checkResult() != null) { } // OK

                    try { checkResult() } catch (e: Exception) { } // ERROR 4
                    val ok = try { checkResult() } catch (e: Exception) { } // OK

                    when (checkResult()) { } // OK
                    when (ok) { true -> checkResult() } // ERROR 5
                    when (ok) { true -> { checkResult() } } // ERROR 6
                    val ok2 = when (ok) { true -> checkResult(); else -> { }} // OK
                    val ok3 = when (ok) { true -> { checkResult() }; else -> { }} // OK

                    when { ok == true -> checkResult(); else -> { }} // ERROR 7
                    val ok4 = when { ok == true -> checkResult(); else -> { }} // OK

                    // b/189978180
                    when {
                        condition1 -> checkReturn()     // ERROR 8
                        condition2 -> { checkReturn() } // ERROR 9
                    }
                }
                """
            ),
            kotlin(
                """
                @file:Suppress("RedundantNullableReturnType")
                package test.pkg

                import androidx.annotation.CheckResult

                @CheckResult
                fun checkResult(): Number? = 42

                @CheckResult
                fun checkReturn(): Any = "test"
                """
            ).indented(),
            SUPPORT_ANNOTATIONS_JAR
        ).run().expect(
            """
            src/test/pkg/test.kt:10: Warning: The result of checkResult is not used [CheckResult]
                                if (true) checkResult()     // ERROR 1
                                          ~~~~~~~~~~~~~
            src/test/pkg/test.kt:11: Warning: The result of checkResult is not used [CheckResult]
                                if (true) { checkResult() } // ERROR 2
                                            ~~~~~~~~~~~~~
            src/test/pkg/test.kt:12: Warning: The result of checkResult is not used [CheckResult]
                                if (true) { } else { checkResult() } // ERROR 3
                                                     ~~~~~~~~~~~~~
            src/test/pkg/test.kt:15: Warning: The result of checkResult is not used [CheckResult]
                                try { checkResult() } catch (e: Exception) { } // ERROR 4
                                      ~~~~~~~~~~~~~
            src/test/pkg/test.kt:19: Warning: The result of checkResult is not used [CheckResult]
                                when (ok) { true -> checkResult() } // ERROR 5
                                                    ~~~~~~~~~~~~~
            src/test/pkg/test.kt:20: Warning: The result of checkResult is not used [CheckResult]
                                when (ok) { true -> { checkResult() } } // ERROR 6
                                                      ~~~~~~~~~~~~~
            src/test/pkg/test.kt:24: Warning: The result of checkResult is not used [CheckResult]
                                when { ok == true -> checkResult(); else -> { }} // ERROR 7
                                                     ~~~~~~~~~~~~~
            src/test/pkg/test.kt:29: Warning: The result of checkReturn is not used [CheckResult]
                                    condition1 -> checkReturn()     // ERROR 8
                                                  ~~~~~~~~~~~~~
            src/test/pkg/test.kt:30: Warning: The result of checkReturn is not used [CheckResult]
                                    condition2 -> { checkReturn() } // ERROR 9
                                                    ~~~~~~~~~~~~~
            0 errors, 9 warnings
            """
        )
    }

    private val javaxCheckReturnValueSource = java(
        """
        package javax.annotation;
        import static java.lang.annotation.RetentionPolicy.CLASS;
        import java.lang.annotation.Retention;
        @SuppressWarnings("ClassNameDiffersFromFileName")
        @Retention(CLASS)
        public @interface CheckReturnValue {
        }
        """
    ).indented()

    private val errorProneCheckReturnValueSource = java(
        """
        package com.google.errorprone.annotations;

        import java.lang.annotation.Documented;
        import java.lang.annotation.Retention;
        import java.lang.annotation.Target;
        import static java.lang.annotation.ElementType.CONSTRUCTOR;
        import static java.lang.annotation.ElementType.METHOD;
        import static java.lang.annotation.ElementType.PACKAGE;
        import static java.lang.annotation.ElementType.TYPE;
        import static java.lang.annotation.RetentionPolicy.RUNTIME;

        @SuppressWarnings("ClassNameDiffersFromFileName")
        @Documented
        @Target({METHOD, CONSTRUCTOR, TYPE, PACKAGE})
        @Retention(RUNTIME)
        public @interface CheckReturnValue {
        }
        """
    )
}

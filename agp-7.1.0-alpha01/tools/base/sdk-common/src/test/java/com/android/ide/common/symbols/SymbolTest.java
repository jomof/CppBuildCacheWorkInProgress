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

package com.android.ide.common.symbols;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.android.resources.ResourceType;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.Test;

public class SymbolTest {

    @Test
    public void checkMakeValidName() {
        // Copy aapt's behaviour
        assertThat("android_a_b_c")
                .isEqualTo(SymbolUtils.canonicalizeValueResourceName("android:a.b.c"));
    }

    @Test
    public void symbolData() {
        Symbol s = SymbolTestUtils.createSymbol("attr", "a", "int", "0xc");
        assertThat(s).isInstanceOf(Symbol.AttributeSymbol.class);
        assertThat(s.getCanonicalName()).isEqualTo("a");
        assertThat(s.getJavaType()).isEqualTo(SymbolJavaType.INT);
        assertThat(s.getIntValue()).isEqualTo(0xc);
        assertThat(s.getResourceType()).isEqualTo(ResourceType.ATTR);
    }

    @Test
    public void namesCannotContainSpaces() {
        try {
            SymbolTestUtils.createSymbol("attr", "a a", "int", "c");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                    .contains("Validation of a resource with name 'a a' and type 'attr' failed.'");
            assertThat(e.getCause().getMessage())
                    .contains("Error: ' ' is not a valid resource name character");
        }
    }

    @Test
    public void valuesCanContainSpaces() {
        SymbolTestUtils.createSymbol("attr", "a", "int", "c c");
    }

    @Test
    public void nameCannotBeEmpty() {
        try {
            SymbolTestUtils.createSymbol("attr", "", "int", "c");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                    .contains("Validation of a resource with name '' and type 'attr' failed.'");
            assertThat(e.getCause().getMessage())
                    .contains("Error: The resource name shouldn't be empty");
        }
    }

    @Test
    public void nameCannotBeNull() {
        try {
            // noinspection ConstantConditions
            SymbolTestUtils.createSymbol("attr", null, "int", "");
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("name");
        }
    }

    @Test
    public void nameCanContainDots() {
        Symbol symbol = SymbolTestUtils.createSymbol("attr", "b.c", "int", "e");
        assertThat(symbol.getName()).isEqualTo("b.c");
        assertThat(symbol.getCanonicalName()).isEqualTo("b_c");
    }

    @Test
    public void nameCannotContainColons() {
        try {
            SymbolTestUtils.createSymbol("attr", "b:c", "int", "e");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                    .contains("Validation of a resource with name 'b:c' and type 'attr' failed.'");
            assertThat(e.getCause().getMessage())
                    .contains("':' is not a valid resource name character");
        }
    }

    /** Test to prevent inadvertent addition of fields which might regress memory use. */
    @Test
    public void checkFieldCounts() {
        Symbol normalSymbol = Symbol.normalSymbol(ResourceType.STRING, "foo");
        assertThat(getAllInstanceFields(normalSymbol.getClass()))
                .containsExactly(
                        "com.android.resources.ResourceType resourceType",
                        "java.lang.String name");

        Symbol attrSymbol = Symbol.attributeSymbol("bar");
        assertThat(getAllInstanceFields(attrSymbol.getClass()))
                .containsExactly("java.lang.String name");

        Symbol styleableSymbol = Symbol.styleableSymbol("baz", ImmutableList.of("bar"));
        assertThat(getAllInstanceFields(styleableSymbol.getClass()))
                .containsExactly(
                        "java.lang.String name",
                        "com.google.common.collect.ImmutableList<java.lang.String> children");
    }

    private static List<String> getAllInstanceFields(Class<?> klass) {
        ImmutableList.Builder<String> fields = ImmutableList.builder();
        while (klass != Object.class) {
            for (Field field : klass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    fields.add(field.getGenericType().getTypeName() + " " + field.getName());
                }
            }
            klass = klass.getSuperclass();
        }
        return fields.build();
    }
}

/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("deprecation", "LombokKotlinCompilerPlugin")

package com.yourorg

import org.junit.jupiter.api.Test
import org.openrewrite.DocumentExample
import org.openrewrite.java.Assertions.java
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class UseModernCharacterChecksTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UseModernCharacterChecks)
    }

    @Suppress("LombokKotlinCompilerPlugin")
    @DocumentExample
    @Test
    fun rewritesKotlin() = rewriteRun(
        kotlin(
            """
            fun blank(c: Char): Boolean = Character.isSpace(c)
            """,
            """
            fun blank(c: Char): Boolean = Character.isWhitespace(c)
            """
        )
    )

    // The `before` lambda names a pure-Java API, so the generated recipe matches Java call sites too.
    @Test
    fun rewritesJava() = rewriteRun(
        java(
            """
            class Chars {
                boolean blank(char c) {
                    return Character.isSpace(c);
                }
            }
            """,
            """
            class Chars {
                boolean blank(char c) {
                    return Character.isWhitespace(c);
                }
            }
            """
        )
    )

    @Test
    fun rewritesTheOtherRenamedPredicates() = rewriteRun(
        kotlin(
            """
            fun start(c: Char): Boolean = Character.isJavaLetter(c)
            fun part(c: Char): Boolean = Character.isJavaLetterOrDigit(c)
            """,
            """
            fun start(c: Char): Boolean = Character.isJavaIdentifierStart(c)
            fun part(c: Char): Boolean = Character.isJavaIdentifierPart(c)
            """
        )
    )

    @Test
    fun leavesUnrelatedCallsAlone() = rewriteRun(
        kotlin(
            """
            fun digit(c: Char): Boolean = Character.isDigit(c)
            """
        )
    )
}

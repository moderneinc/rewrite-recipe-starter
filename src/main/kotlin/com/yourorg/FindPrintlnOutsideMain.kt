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
package com.yourorg

import org.openrewrite.Recipe
import org.openrewrite.java.MethodMatcher
import org.openrewrite.java.tree.J
import org.openrewrite.marker.SearchResult
import org.openrewrite.recipe

private const val PRINTLN = "kotlin.io.ConsoleKt println(..)"

private val PRINTLN_MATCHER = MethodMatcher(PRINTLN)

/**
 * The imperative half of the Kotlin recipe DSL. A `rewrite { } to { }` clause is a fixed
 * before/after pair, so it cannot express a change that depends on where the code appears;
 * `kotlin { visitX { } }` opens a real `KotlinVisitor` for that, with the cursor available as
 * a receiver. One language scope per language: `java { }`, `yaml { }`, `xml { }` and the rest
 * work the same way.
 *
 * Wrapping the visitor in `check(...)` applies a precondition: source files with no `println`
 * call at all skip the visitor entirely. `usesMethod`/`uses<T>()`/`usesField` build the usual
 * precondition visitors, and `and`/`or`/`not` combine them.
 */
val FindPrintlnOutsideMain: Recipe = recipe(
    displayName = "Find `println` calls outside of `main`",
    description = "Marks console output that should probably go through a logging framework instead, " +
        "leaving calls in a `main` function alone.",
    tags = setOf("logging"),
) {
    edit {
        check(
            usesMethod(PRINTLN),
            kotlin {
                visitMethodInvocation { method ->
                    if (!PRINTLN_MATCHER.matches(method)) {
                        return@visitMethodInvocation method
                    }
                    // Kotlin top-level and member functions alike are J.MethodDeclaration in the LST.
                    val enclosing = cursor.firstEnclosing(J.MethodDeclaration::class.java)
                    if (enclosing?.simpleName == "main") {
                        return@visitMethodInvocation method
                    }
                    SearchResult.found(method, "prefer a logging framework") ?: method
                }
            },
        )
    }
}

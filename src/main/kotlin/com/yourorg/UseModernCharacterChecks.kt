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
@file:Suppress("DEPRECATION")

package com.yourorg

import org.openrewrite.Recipe
import org.openrewrite.recipe
import org.openrewrite.recipes

/**
 * The declarative half of the Kotlin recipe DSL: each `rewrite { } to { }` clause is an ordinary
 * pair of Kotlin lambdas, type-checked by the compiler against the real APIs it names. The
 * rewrite-kotlin K2 compiler plugin reads the resolved before-lambda, derives a `MethodMatcher`
 * from it, builds a template from the after-lambda, and synthesizes the `Recipe` subclass.
 *
 * Because these three target pure-Java APIs, the generated recipes rewrite Java sources as well as
 * Kotlin ones; see `UseModernCharacterChecksTest`.
 *
 * The plugin names the generated recipe after the property, suffixed with `$KtRecipe`, e.g.
 * `com.yourorg.UseSpaceCheck$KtRecipe`.
 */
val UseSpaceCheck: Recipe = recipe(
    displayName = "Use `Character.isWhitespace(char)` instead of `Character.isSpace(char)`",
    description = "`Character.isSpace(char)` was deprecated in Java 1.1; " +
        "`Character.isWhitespace(char)` recognizes Unicode whitespace as well.",
) {
    edit {
        rewrite { c: Char -> Character.isSpace(c) } to { c -> Character.isWhitespace(c) }
    }
}

val UseIdentifierStartCheck: Recipe = recipe(
    displayName = "Use `Character.isJavaIdentifierStart(char)` instead of `Character.isJavaLetter(char)`",
    description = "`Character.isJavaLetter(char)` was deprecated in Java 1.1 and renamed.",
) {
    edit {
        rewrite { c: Char -> Character.isJavaLetter(c) } to { c -> Character.isJavaIdentifierStart(c) }
    }
}

val UseIdentifierPartCheck: Recipe = recipe(
    displayName = "Use `Character.isJavaIdentifierPart(char)` instead of `Character.isJavaLetterOrDigit(char)`",
    description = "`Character.isJavaLetterOrDigit(char)` was deprecated in Java 1.1 and renamed.",
) {
    edit {
        rewrite { c: Char -> Character.isJavaLetterOrDigit(c) } to { c -> Character.isJavaIdentifierPart(c) }
    }
}

/**
 * `recipes(...)` composes the fine-grained recipes above into a single migration target, the
 * Kotlin equivalent of a declarative YAML recipe with a `recipeList`.
 */
val UseModernCharacterChecks: Recipe = recipes(
    displayName = "Use modern `Character` predicates",
    description = "Replaces the `Character` methods deprecated in Java 1.1 with their supported equivalents.",
    UseSpaceCheck,
    UseIdentifierStartCheck,
    UseIdentifierPartCheck,
)

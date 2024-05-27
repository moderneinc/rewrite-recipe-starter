/*
 * Copyright 2024 the original author or authors.
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
package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

// This is a test for the UseOpenRewriteNullable recipe, as an example of how to write a test for a declarative recipe.
class UseOpenRewriteNullableTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          // Use the fully qualified class name of the recipe defined in src/main/resources/META-INF/rewrite/rewrite.yml
          .recipeFromResources("com.yourorg.UseOpenRewriteNullable")
          // The before and after text blocks contain references to annotations from these two classpath entries
          .parser(JavaParser.fromJavaVersion().classpath("annotations", "rewrite-core"));
    }

    @DocumentExample
    @Test
    void replacesNullableAnnotation() {
        rewriteRun(
          // Composite recipes are a hierarchy of recipes that can be applied in a single pass.
          // To view what the composite recipe does, you can use the RecipePrinter to print the recipe to the console.
          spec -> spec.printRecipe(() -> System.out::println),
          //language=java
          java(
            """
              import org.jetbrains.annotations.Nullable;
              
              class A {
                  @Nullable
                  String s;
              }
              """,
            """
              import org.openrewrite.internal.lang.Nullable;
              
              class A {
                  @Nullable
                  String s;
              }
              """
          )
        );
    }
}

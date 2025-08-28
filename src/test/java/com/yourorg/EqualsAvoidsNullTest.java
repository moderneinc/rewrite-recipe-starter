/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class EqualsAvoidsNullTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new EqualsAvoidsNullRecipe());
    }

    @DocumentExample
    @Test
    void beforeEquals() {
        rewriteRun(
          java(
            """
              class Foo {
                  boolean unsafe(String actual) {
                      return actual.equals("literal");
                  }

                  boolean nullsafe(String actual) {
                      return actual != null && actual.equals("literal");
                  }
              }
              """,
            """
              class Foo {
                  boolean unsafe(String actual) {
                      return "literal".equals(actual);
                  }

                  boolean nullsafe(String actual) {
                      return "literal".equals(actual);
                  }
              }
              """
          )
        );
    }

    @Test
    void unchanged() {
        rewriteRun(
          java(
            """
              class Foo {
                  String field = "field";

                  void unchanged(String actual, String other) {
                      // Already safe
                      boolean a = "literal".equals(actual);

                      // Odd, but not changed
                      boolean b = "other".equals("literal");

                      // Not safe to change, as right side is not a literal, and could be null
                      boolean c = actual.equals(field);
                      boolean d = actual.equals(method());
                      boolean e = actual.equals(other);

                      // Null checked variants also not changed
                      boolean f = actual != null && actual.equals(field);
                      boolean g = actual != null && actual.equals(method());
                      boolean h = actual != null && actual.equals(other);
                  }

                  String method() {
                      return "method";
                  }
              }
              """
          )
        );
    }
}

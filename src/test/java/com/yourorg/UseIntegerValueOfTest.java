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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Disabled("Remove this annotation to run the tests once you implement the recipe")
class UseIntegerValueOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseIntegerValueOf());
    }

    @DocumentExample
    @Test
    void replacesNewIntegerWithValueOf() {
        rewriteRun(
          java(
            """
            class Test {
                Integer i = new Integer(42);
            }
            """,
            """
            class Test {
                Integer i = Integer.valueOf(42);
            }
            """
          )
        );
    }

    @Test
    void replacesNewIntegerWithParseInt() {
        rewriteRun(
          java(
            """
            class Test {
                Integer i = new Integer("42");
            }
            """,
            """
            class Test {
                Integer i = Integer.parseInt("42");
            }
            """
          )
        );
    }
}

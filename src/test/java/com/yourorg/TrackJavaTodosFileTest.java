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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.text;

class TrackJavaTodosFileTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TrackJavaTodosFile("## Test Header"));
    }

    @Disabled
    @DocumentExample
    @Test
    void createNewTodoFile() {
        // Notice how the before text is doesNotExist(), indicating that the file does not exist yet.
        // The after text is the content of the file after the recipe is applied.
        rewriteRun(
          //language=java
          java(
            """
          class A {
              // TODO: Have fun
              /* TODO: Test your code */
              // Just a regular comment
              public String foo() {
                // TODO: Learn
                return "bar";
              }
              // Another regular comment
          }
          """
          ),
          //language=markdown
          text(
            doesNotExist(),
            """
              ## Test Header
              TODO: Have fun
              TODO: Test your code
              TODO: Learn
              """,
            spec -> spec.path(Path.of("TODO.md"))
          )
        );
    }

    @Disabled
    @Test
    void editExistingTodoFile() {
        // When the file does already exist, we assert the content is modified as expected.
        rewriteRun(
          //language=java
          java(
            """
          class A {
              // TODO: Have fun
              /* TODO: Test your code */
              // Just a regular comment
              public String foo() {
                // TODO: Learn
                return "bar";
              }
              // Another regular comment
          }
          """
          ),
          //language=markdown
          text(
            "",
            """
              ## Test Header
              TODO: Have fun
              TODO: Test your code
              TODO: Learn
              """,
            spec -> spec.path(Path.of("TODO.md"))
          )
        );
    }

    @Disabled
    @Test
    void doNotTouchExistingCorrectFile() {
        // When the file does already exist and is equal, we assert no changes are made by not having an after String.
        rewriteRun(
          //language=java
          java(
            """
          class A {
              // TODO: Have fun
              /* TODO: Test your code */
              // Just a regular comment
              public String foo() {
                // TODO: Learn
                return "bar";
              }
              // Another regular comment
          }
          """
          ),
          //language=markdown
          text(
            """
            ## Test Header
            TODO: Have fun
            TODO: Test your code
            TODO: Learn
            """,
            spec -> spec.path(Path.of("TODO.md"))
          )
        );
    }
}

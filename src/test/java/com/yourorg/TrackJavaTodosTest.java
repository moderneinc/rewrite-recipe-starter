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

import com.yourorg.table.TodoCommentsReport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class TrackJavaTodosTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TrackJavaTodos());
    }

    @Disabled
    @DocumentExample
    @Test
    void findTodos() {
        // Notice how the before text is doesNotExist(), indicating that the file does not exist yet.
        // The after text is the content of the file after the recipe is applied.
        rewriteRun(
          spec -> spec.dataTable(TodoCommentsReport.Row.class, rows ->
            assertThat(rows)
              .containsExactly(
                new TodoCommentsReport.Row("A.java", "TODO: Have fun", "class org.openrewrite.java.tree.J$MethodDeclaration"),
                new TodoCommentsReport.Row("A.java", "TODO: Test your code", "class org.openrewrite.java.tree.J$MethodDeclaration"),
                new TodoCommentsReport.Row("A.java", "TODO: Learn", "class org.openrewrite.java.tree.J$Return"))),
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
          )
        );
    }
}

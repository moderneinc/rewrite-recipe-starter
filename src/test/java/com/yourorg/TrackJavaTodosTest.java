package com.yourorg;

import com.yourorg.table.TodoCommentsReport;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.text;

class TrackJavaTodosTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TrackJavaTodos());
    }

    @DocumentExample
    @Test
    void findTodos() {
        // Notice how the before text is doesNotExist(), indicating that the file does not exist yet.
        // The after text is the content of the file after the recipe is applied.
        rewriteRun(
          spec -> spec.dataTable(TodoCommentsReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new TodoCommentsReport.Row("A.java", "TODO: Have fun", "class org.openrewrite.java.tree.J$MethodDeclaration"),
                new TodoCommentsReport.Row("A.java", "TODO: Test your code", "class org.openrewrite.java.tree.J$MethodDeclaration"),
                new TodoCommentsReport.Row("A.java", "TODO: Learn", "class org.openrewrite.java.tree.J$Return"));
          }),
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

package com.yourorg;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.text;

class TrackJavaTodosTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TrackJavaTodos("## Test Header"));
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
                    spec -> spec.path(Path.of("TODO.md")).noTrim()
            )
        );
    }

    @Disabled
    @Test
    void editExistingTodoFile() {
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
                      """ + "\n",
                    spec -> spec.path(Path.of("TODO.md")).noTrim()
            )
        );
    }

    @Disabled
    @Test
    void doNotTouchExistingCorrectFile() {
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
                    spec -> spec.path(Path.of("TODO.md")).noTrim()
            )
        );
    }
}

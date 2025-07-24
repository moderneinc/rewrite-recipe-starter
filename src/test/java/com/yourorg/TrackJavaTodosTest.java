package com.yourorg;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.java.Assertions.java;

class TrackJavaTodosTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TrackJavaTodos("## Test Header"));
    }

    @DocumentExample
    @Test
    @Disabled
    void createNewTodoFile() {
        rewriteRun(
                //language=java
                java("""
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
              """),
                //language=markdown
                text(
                        doesNotExist(),
                        """
                          ## Test Header
                          TODO: Have fun
                          TODO: Test your code
                          TODO: Learn
                          """,
                        spec -> spec.path(Path.of("TODO.md")
                        )
                )
        );
    }

    @Test
    @Disabled
    void editExistingTodoFile() {
        rewriteRun(
                //language=java
                java("""
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
              """),
                //language=markdown
                text(
                        "",
                        """
                          ## Test Header
                          TODO: Have fun
                          TODO: Test your code
                          TODO: Learn
                          """ + "\n",
                        spec -> spec.path(Path.of("TODO.md")
                        )
                )
        );
    }
}
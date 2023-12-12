package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class AssertEqualsToAssertThatTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AssertEqualsToAssertThat())
          .parser(JavaParser.fromJavaVersion()
            .classpath("junit-jupiter-api"));
    }

    @Test
    void twoArgument() {
        rewriteRun(
          //language=java
          java("""
            import org.junit.jupiter.api.Assertions;
            
            class A {
                void foo() {
                    Assertions.assertEquals(1, 2);
                }
            }
            """,
            """
            import org.assertj.core.api.Assertions;
            
            class A {
                void foo() {
                    Assertions.assertThat(2).isEqualTo(1);
                }
            }
            """)
        );
    }


    @Test
    void withDescription() {
        rewriteRun(
          //language=java
          java("""
            import org.junit.jupiter.api.Assertions;
            
            class A {
                void foo() {
                    Assertions.assertEquals(1, 2, "one equals two, everyone knows that");
                }
            }
            """,
            """
            import org.assertj.core.api.Assertions;
            
            class A {
                void foo() {
                    Assertions.assertThat(2).as("one equals two, everyone knows that").isEqualTo(1);
                }
            }
            """)
        );
    }
}

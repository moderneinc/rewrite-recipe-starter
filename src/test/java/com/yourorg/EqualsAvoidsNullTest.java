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

                      // Not safe to change
                      boolean c = field.equals(other);
                      boolean d = method().equals(other);
                      boolean e = other.equals(actual);
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
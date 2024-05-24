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
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Disabled("Remove this annotation to run the tests once you implement the recipe")
class StringIsEmptyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // Note that we instantiate a generated class here, with `Recipes` appended to the Refaster class name
        // You might need to trigger an explicit build of your project to generate this class with Ctrl + F9

        // TODO: Uncomment the line below once you have implemented the recipe
        //spec.recipe(new StringIsEmptyRecipe());
    }

    @DocumentExample
    @Test
    void standardizeStringIsEmpty() {
        // Notice how we pass in both the "before" and "after" code snippets
        // This indicates that we expect the recipe to transform the "before" code snippet into the "after" code snippet
        // If the recipe does not do this, the test will fail, and a diff will be shown
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  void test(String s, boolean b) {
                      b = s.length() == 0;
                      b = 0 == s.length();
                      b = s.length() < 1;
                      b = 1 > s.length();
                      b = s.equals("");
                      b = "".equals(s);
                      b = s.isEmpty();
                  }
              }
              """,
            """
              class A {
                  void test(String s, boolean b) {
                      b = s.isEmpty();
                      b = s.isEmpty();
                      b = s.isEmpty();
                      b = s.isEmpty();
                      b = s.isEmpty();
                      b = s.isEmpty();
                      b = s.isEmpty();
                  }
              }
              """
          )
        );
    }

    @Test
    void showStringTypeMatchAndSimplification() {
        // Notice how the recipe will match anything that is of type String, not just local variables
        // Take a closer look at the last two replacements to `true` and `false`.
        // Open up the generated recipe and see if you can work out why those are replaced with booleans!
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  String field;
              
                  String methodCall() {
                      return "Hello World";
                  }
              
                  void test(String argument) {
                      boolean bool1 = field.length() == 0;
                      boolean bool2 = methodCall().length() == 0;
                      boolean bool3 = argument.length() == 0;
                      boolean bool4 = "".length() == 0;
                      boolean bool5 = "literal".length() == 0;
                  }
              }
              """,
            """
              class A {
                  String field;
              
                  String methodCall() {
                      return "Hello World";
                  }
              
                  void test(String argument) {
                      boolean bool1 = field.isEmpty();
                      boolean bool2 = methodCall().isEmpty();
                      boolean bool3 = argument.isEmpty();
                      boolean bool4 = true;
                      boolean bool5 = false;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNothingForStringIsEmpty() {
        // Notice how we only pass in the "before" code snippet, and not the "after" code snippet
        // That indicates that we expect the recipe to do nothing in this case, and will fail if it does anything
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  void test(String s, boolean b) {
                      b = s.isEmpty();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNothingForCharSequence() {
        // When a different type is used, the recipe should do nothing
        // See if you can modify the recipe to handle CharSequence as well, or create a separate recipe for it
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  void test(CharSequence s, boolean b) {
                      b = s.length() == 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void recipeDocumentation() {
        // This is a test to validate the correctness of the documentation in the recipe
        // By default you get generated documentation, but you can customize it through the RecipeDescriptor annotation
        Recipe recipe = null; // TODO: = new StringIsEmptyRecipe();
        String displayName = recipe.getDisplayName();
        String description = recipe.getDescription();
        assert "Standardize empty String checks".equals(displayName) : displayName;
        assert "Replace calls to `String.length() == 0` with `String.isEmpty()`.".equals(description) : description;
    }
}

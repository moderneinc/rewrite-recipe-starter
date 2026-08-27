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

@SuppressWarnings({"StringBufferReplaceableByString", "StringBufferMayBeStringBuilder", "UnnecessaryLocalVariable", "UnnecessaryToStringCall"})
class UseStringBuilderForLocalStringBufferTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseStringBuilderForLocalStringBuffer());
    }

    @DocumentExample
    @Test
    void stringBufferThatNeverEscapes() {
        rewriteRun(
          java(
            """
              class Greeter {
                  String greet(String name) {
                      StringBuffer sb = new StringBuffer();
                      sb.append("Hello, ").append(name);
                      sb.append('!');
                      return sb.toString();
                  }
              }
              """,
            """
              class Greeter {
                  String greet(String name) {
                      StringBuilder sb = new StringBuilder();
                      sb.append("Hello, ").append(name);
                      sb.append('!');
                      return sb.toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void returnedStringBufferEscapes() {
        rewriteRun(
          java(
            """
              class Greeter {
                  StringBuffer greet(String name) {
                      StringBuffer sb = new StringBuffer();
                      sb.append("Hello, ").append(name);
                      return sb;
                  }
              }
              """
          )
        );
    }

    @Test
    void stringBufferPassedToAnotherMethodEscapes() {
        rewriteRun(
          java(
            """
              class Greeter {
                  void greet(String name) {
                      StringBuffer sb = new StringBuffer();
                      sb.append("Hello, ").append(name);
                      share(sb);
                  }

                  void share(StringBuffer shared) {
                  }
              }
              """
          )
        );
    }

    @Test
    void stringBufferAssignedToFieldEscapes() {
        rewriteRun(
          java(
            """
              class Greeter {
                  StringBuffer shared;

                  void greet(String name) {
                      StringBuffer sb = new StringBuffer();
                      sb.append("Hello, ").append(name);
                      shared = sb;
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldIsNotALocal() {
        rewriteRun(
          java(
            """
              class Greeter {
                  private final StringBuffer sb = new StringBuffer();

                  void greet(String name) {
                      sb.append("Hello, ").append(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void aliasedIntoAnotherStringBufferLocalEscapes() {
        rewriteRun(
          java(
            """
              class Greeter {
                  String greet(String name) {
                      StringBuffer sb = new StringBuffer();
                      StringBuffer alias = sb;
                      alias.append("Hello, ").append(name);
                      return sb.toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleVariablesInOneDeclarationAreLeftAlone() {
        rewriteRun(
          java(
            """
              class Greeter {
                  String greet(String name) {
                      StringBuffer sb = new StringBuffer(), other = new StringBuffer();
                      sb.append("Hello, ").append(name);
                      return sb.toString() + other.toString();
                  }
              }
              """
          )
        );
    }
}

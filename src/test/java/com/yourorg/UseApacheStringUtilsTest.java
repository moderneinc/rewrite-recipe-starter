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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseApacheStringUtilsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("com.yourorg.UseApacheStringUtils")
          // Notice how we only pass in `spring-core` as the classpath, but not `commons-lang3`.
          // That's because we only need dependencies to compile the before code blocks, not the after code blocks.
          .parser(JavaParser.fromJavaVersion().classpath("spring-core"));
    }

    @DocumentExample
    @Test
    void replacesStringEquals() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.util.StringUtils;

              class A {
                  boolean test(String s) {
                      return StringUtils.containsWhitespace(s);
                  }
              }
              """,
            """
              import org.apache.commons.lang3.StringUtils;

              class A {
                  boolean test(String s) {
                      return StringUtils.containsWhitespace(s);
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenAlreadyUsingCommonsLang3() {
        rewriteRun(
          // By passing in `commons-lang3` as the classpath here, we ensure that the before code block compiles.
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("commons-lang3")),
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;

              class A {
                  boolean test(String s) {
                      return StringUtils.containsWhitespace(s);
                  }
              }
              """
            // The absence of a second argument to `java` indicates that the after code block should be the same as the before code block.
          )
        );
    }
}

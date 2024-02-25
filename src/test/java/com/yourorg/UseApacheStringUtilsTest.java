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
          .parser(JavaParser.fromJavaVersion().classpath("commons-lang3", "spring-core"));
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
}

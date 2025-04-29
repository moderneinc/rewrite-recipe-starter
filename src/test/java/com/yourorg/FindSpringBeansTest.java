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

import com.yourorg.table.SpringBeansReport;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FindSpringBeansTest implements RewriteTest {

    // Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    // In this case, the recipe and the parser are common.
    @Override
    public void defaults(RecipeSpec spec) {
        // Note how we directly instantiate the recipe class here
        spec.recipe(new FindSpringBeans())
          // The before examples are using Spring classes/annotations, so we need to add spring-context to the classpath
          .parser(JavaParser.fromJavaVersion().classpath("spring-context"));
    }

    @DocumentExample
    @Test
    void findSpringBeans() {
        String filePath = "src/main/java/com/yourorg/MyConfig.java";
        rewriteRun(
          spec -> spec.dataTable(SpringBeansReport.Row.class, rows ->
            assertThat(rows)
              .containsExactly(
                new SpringBeansReport.Row(filePath, "bean"),
                new SpringBeansReport.Row(filePath, "namedBean"),
                new SpringBeansReport.Row(filePath, "useMethodNameWhenNoValuePresent")
              )),
          java(
            """
              import org.springframework.context.annotation.Bean;
              import org.springframework.context.annotation.Configuration;
              
              @Configuration
              public class MyConfig {
                  @Bean("bean")
                  public String doNotUseMethodNameWhenDefaultIsPresent() {
                      return "Named Bean";
                  }
              
                  @Bean(name = "namedBean")
                  public String doNotUseMethodNameWhenNameIsPresent() {
                      return "Named Bean";
                  }
              
                  @Bean
                  public String useMethodNameWhenNoValuePresent() {
                      return "Named Bean";
                  }
              
                  @Override
                  public String doNotListOtherMethods() {
                      return "Private method";
                  }
              }
              """,
            """
              import org.springframework.context.annotation.Bean;
              import org.springframework.context.annotation.Configuration;
              
              @Configuration
              public class MyConfig {
                  /*~~(bean)~~>*/@Bean("bean")
                  public String doNotUseMethodNameWhenDefaultIsPresent() {
                      return "Named Bean";
                  }
              
                  /*~~(namedBean)~~>*/@Bean(name = "namedBean")
                  public String doNotUseMethodNameWhenNameIsPresent() {
                      return "Named Bean";
                  }
              
                  /*~~(useMethodNameWhenNoValuePresent)~~>*/@Bean
                  public String useMethodNameWhenNoValuePresent() {
                      return "Named Bean";
                  }
              
                  @Override
                  public String doNotListOtherMethods() {
                      return "Private method";
                  }
              }
              """,
            spec -> spec.path(filePath)
          )
        );
    }
}

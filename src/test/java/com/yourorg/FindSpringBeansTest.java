package com.yourorg;

import com.yourorg.table.SpringBeans;
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
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            // The before/after examples are using Spring classes/annotations, so we need to add the spring-context library to the classpath
            .classpath("spring-context"));
    }

    @DocumentExample
    @Test
    void findSpringBeans() {
        String filePath = "src/main/java/com/yourorg/MyConfig.java";
        rewriteRun(
          spec -> spec.dataTable(SpringBeans.Row.class, rows ->
            assertThat(rows)
              .containsExactly(
                new SpringBeans.Row(filePath, "bean"),
                new SpringBeans.Row(filePath, "namedBean"),
                new SpringBeans.Row(filePath, "useMethodNameWhenNoValuePresent")
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

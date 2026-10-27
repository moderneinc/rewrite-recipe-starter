package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class BootstrapIntoApplicationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new BootstrapIntoApplication(false));
    }

    @DocumentExample
    @Test
    void mergeWithExistingApplication() {
        rewriteRun(
          yaml(
            //language=yaml
            """
              spring:
                application:
                  name: "foo"
              """,
            spec -> spec.path("src/main/resources/bootstrap.yml")
          ),
          yaml(
            //language=yaml
            """
              spring:
                foo: bar
              """,
            //language=yaml
            """
              spring:
                foo: bar
                application:
                  name: "foo"
              """,
            spec -> spec.path("src/main/resources/application.yml")
          )
        );
    }

    @Test
    void createNewApplicationYaml() {
        rewriteRun(
          yaml(
            //language=yaml
            """
              spring:
                application:
                  name: "foo"
              """,
            spec -> spec.path("src/main/resources/bootstrap.yml")
          ),
          // Use "null" as the "before" argument to assert that the recipe creates a file
          yaml(
            doesNotExist(),
            //language=yaml
            """
              spring:
                application:
                  name: "foo"
              """,
            spec -> spec.path("src/main/resources/application.yml")
          )
        );
    }

    @Test
    void pairFilesPerModule() {
        rewriteRun(
          yaml(
            //language=yaml
            """
              spring:
                application:
                  name: "a"
              """,
            spec -> spec.path("module-a/src/main/resources/bootstrap.yml")
          ),
          yaml(
            //language=yaml
            """
              spring:
                foo: bar
              """,
            //language=yaml
            """
              spring:
                foo: bar
                application:
                  name: "a"
              """,
            spec -> spec.path("module-a/src/main/resources/application.yml")
          ),
          yaml(
            //language=yaml
            """
              spring:
                application:
                  name: "b"
              """,
            spec -> spec.path("module-b/src/main/resources/bootstrap.yml")
          ),
          yaml(
            doesNotExist(),
            //language=yaml
            """
              spring:
                application:
                  name: "b"
              """,
            spec -> spec.path("module-b/src/main/resources/application.yml")
          )
        );
    }

    @Test
    void deleteOldBootstrap() {
        rewriteRun(
          spec -> spec.recipe(new BootstrapIntoApplication(true)),
          yaml(
            //language=yaml
            """
              spring:
                application:
                  name: "foo"
              """,
            doesNotExist(),
            spec -> spec.path("src/main/resources/bootstrap.yml")
          ),
          yaml(
            doesNotExist(),
            //language=yaml
            """
              spring:
                application:
                  name: "foo"
              """,
            spec -> spec.path("src/main/resources/application.yml")
          )
        );
    }
}

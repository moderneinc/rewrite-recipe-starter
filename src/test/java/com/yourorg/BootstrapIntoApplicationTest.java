package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptyList;
import static org.openrewrite.yaml.Assertions.yaml;

class BootstrapIntoApplicationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new BootstrapIntoApplication(false));
    }

    @DocumentExample
    @Test
    void mergeWithExistingApplication() {
        JavaSourceSet main = JavaSourceSet.build("main", emptyList());
        rewriteRun(
          yaml(
            //language=yaml
            """
              spring:
                application:
                  name: "foo"
              """,
            spec -> spec.path("bootstrap.yml").markers(main)
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
            spec -> spec.path("application.yml").markers(main)
          )
        );
    }

    @Test
    void createNewApplicationYaml() {
        // The test framework does not automatically add markers like JavaSourceSet or JavaProject
        // These are added during the parsing of real projects, but must be explicitly added to tests
        JavaSourceSet main = JavaSourceSet.build("main", emptyList());
        rewriteRun(
          yaml(
            //language=yaml
            """
              spring:
                application:
                  name: "foo"
              """,
            spec -> spec.path("bootstrap.yml").markers(main)
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
            spec -> spec.path("application.yml")
          )
        );
    }

    @Test
    void deleteOldBootstrap() {
        JavaSourceSet main = JavaSourceSet.build("main", emptyList());
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
            spec -> spec.path("bootstrap.yml").markers(main)
          ),
          yaml(
            doesNotExist(),
            //language=yaml
            """
              spring:
                application:
                  name: "foo"
              """,
            spec -> spec.path("application.yml")
          )
        );
    }
}

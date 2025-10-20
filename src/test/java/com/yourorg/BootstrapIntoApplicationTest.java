package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Collections;

import static org.openrewrite.yaml.Assertions.*;

class BootstrapIntoApplicationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new BootstrapIntoApplication(false));
    }

    @Test
    void createNewApplicationYaml() {
        // The test framework does not automatically add markers like JavaSourceSet or JavaProject
        // These are added during the parsing of real projects, but must be explicitly added to tests
        JavaSourceSet main = JavaSourceSet.build("main", Collections.emptyList());
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
            null,
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
        JavaSourceSet main = JavaSourceSet.build("main", Collections.emptyList());
        rewriteRun(
          spec -> spec.recipe(new BootstrapIntoApplication(true)),
          yaml(
            //language=yaml
            """
              spring:
                application:
                  name: "foo"
              """,
            null,
            spec -> spec.path("bootstrap.yml").markers(main)
          ),
          yaml(
            null,
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
    void mergeWithExistingApplication() {
        JavaSourceSet main = JavaSourceSet.build("main", Collections.emptyList());
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
}

package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Paths;

import static org.openrewrite.test.SourceSpecs.text;

public class AppendToReleaseNotesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AppendToReleaseNotes("Hello world"));
    }

    @Test
    void createNewReleaseNotes() {
        rewriteRun(
          text(null,
            """
              Hello world
              """)
        );
    }

    @Test
    void editExistingReleaseNotes() {
        rewriteRun(
          text("""
              You say goodbye, I say
              """,
            """
              You say goodbye, I say
              Hello world
              """,
            spec -> spec.path(Paths.get("RELEASE.md")))
        );
    }
}

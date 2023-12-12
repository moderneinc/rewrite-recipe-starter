package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.Assertions;

import java.nio.file.Paths;

import static org.openrewrite.yaml.Assertions.yaml;

public class UpdateConcoursePipelineTest implements RewriteTest {

    @Test
    void updateTagFilter() {
        rewriteRun(
          spec -> spec.recipe(new UpdateConcoursePipeline("8.2.0")),
          //language=yaml
          yaml("""
            ---
            resources:
              - name: tasks
                type: git
                source:
                  uri: git@github.com:Example/concourse-tasks.git
                  tag_filter: 8.1.0
            """,
            """
            ---
            resources:
              - name: tasks
                type: git
                source:
                  uri: git@github.com:Example/concourse-tasks.git
                  tag_filter: 8.2.0
              """,
            spec -> spec.path(Paths.get("ci/pipeline.yml")))
        );
    }
}

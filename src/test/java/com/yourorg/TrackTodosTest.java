package com.yourorg;

import com.yourorg.table.TodoCommentsReport;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;
import static org.openrewrite.yaml.Assertions.yaml;

class TrackTodosTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TrackTodos("## Test Header"))
                .allSources(SourceSpec::noTrim);
    }

    @DocumentExample
    @Test
    void createNewTodoFileJava() {
        // Notice how the before text is doesNotExist(), indicating that the file does not exist yet.
        // The after text is the content of the file after the recipe is applied.
        rewriteRun(
          spec -> spec.dataTable(TodoCommentsReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new TodoCommentsReport.Row("A.java", "TODO: Have fun", "class org.openrewrite.java.tree.J$MethodDeclaration"),
                new TodoCommentsReport.Row("A.java", "TODO: Test your code", "class org.openrewrite.java.tree.J$MethodDeclaration"),
                new TodoCommentsReport.Row("A.java", "TODO: Learn", "class org.openrewrite.java.tree.J$Return"));
          }),
          //language=java
          java(
                """
            class A {
                // TODO: Have fun
                /* TODO: Test your code */
                // Just a regular comment
                public String foo() {
                  // TODO: Learn
                  return "bar";
                }
                // Another regular comment
            }
            """
          ),
          //language=markdown
          text(
            doesNotExist(),
            """
              ## Test Header
              TODO: Have fun
              TODO: Test your code
              TODO: Learn
              """,
            spec -> spec.path(Path.of("TODO.md")
            )
          )
        );
    }

    @Test
    void editExistingTodoFileJava() {
        // When the file does already exist, we assert the content is modified as expected.
        rewriteRun(
          spec -> spec.dataTable(TodoCommentsReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new TodoCommentsReport.Row("A.java", "TODO: Have fun", "class org.openrewrite.java.tree.J$MethodDeclaration"),
                new TodoCommentsReport.Row("A.java", "TODO: Test your code", "class org.openrewrite.java.tree.J$MethodDeclaration"),
                new TodoCommentsReport.Row("A.java", "TODO: Learn", "class org.openrewrite.java.tree.J$Return"));
          }),
          //language=java
          java(
                """
            class A {
                // TODO: Have fun
                /* TODO: Test your code */
                // Just a regular comment
                public String foo() {
                  // TODO: Learn
                  return "bar";
                }
                // Another regular comment
            }
            """
          ),
          //language=markdown
          text(
            "",
            """
              ## Test Header
              TODO: Have fun
              TODO: Test your code
              TODO: Learn
              """,
            spec -> spec.path(Path.of("TODO.md")
            )
          )
        );
    }

    @Test
    void createNewTodoFileYAML() {
        // Notice how the before text is doesNotExist(), indicating that the file does not exist yet.
        // The after text is the content of the file after the recipe is applied.
        rewriteRun(
          spec -> spec.dataTable(TodoCommentsReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new TodoCommentsReport.Row("file.yaml", "TODO: Have fun", "class org.openrewrite.yaml.tree.Yaml$Document"),
                new TodoCommentsReport.Row("file.yaml", "TODO: Test your code", "class org.openrewrite.yaml.tree.Yaml$Mapping$Entry"),
                new TodoCommentsReport.Row("file.yaml", "TODO: Learn", "class org.openrewrite.yaml.tree.Yaml$Mapping$Entry"));
          }),
          //language=yaml
          yaml(
                """
            # TODO: Have fun
            someyaml: "here"
            moreyaml: "there"
            # TODO: Test your code
            # TODO: Learn
            # Just a regular comment
            tabs:
              are: "fun"
            """
          ),
          //language=markdown
          text(
            doesNotExist(),
            """
              ## Test Header
              TODO: Have fun
              TODO: Test your code
              TODO: Learn
              """,
            spec -> spec.path(Path.of("TODO.md")
            )
          )
        );
    }

    @Test
    void editExistingTodoFileYAML() {
        // When the file does already exist, we assert the content is modified as expected.
        rewriteRun(
          spec -> spec.dataTable(TodoCommentsReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new TodoCommentsReport.Row("file.yaml", "TODO: Have fun", "class org.openrewrite.yaml.tree.Yaml$Document"),
                new TodoCommentsReport.Row("file.yaml", "TODO: Test your code", "class org.openrewrite.yaml.tree.Yaml$Mapping$Entry"),
                new TodoCommentsReport.Row("file.yaml", "TODO: Learn", "class org.openrewrite.yaml.tree.Yaml$Mapping$Entry"));
          }),
          //language=yaml
          yaml(
                """
            # TODO: Have fun
            someyaml: "here"
            moreyaml: "there"
            # TODO: Test your code
            # TODO: Learn
            # Just a regular comment
            tabs:
              are: "fun"
            """
          ),
          //language=markdown
          text(
            "",
            """
              ## Test Header
              TODO: Have fun
              TODO: Test your code
              TODO: Learn
              """,
            spec -> spec.path(Path.of("TODO.md")
            )
          )
        );
    }

    @Test
    void createNewTodoFileXML() {
        // Notice how the before text is doesNotExist(), indicating that the file does not exist yet.
        // The after text is the content of the file after the recipe is applied.
        rewriteRun(
          spec -> spec.dataTable(TodoCommentsReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new TodoCommentsReport.Row("file.xml", "TODO: Have fun", "class org.openrewrite.xml.tree.Xml$Prolog"),
                new TodoCommentsReport.Row("file.xml", "TODO: Test your code", "class org.openrewrite.xml.tree.Xml$Prolog"),
                new TodoCommentsReport.Row("file.xml", "TODO: Learn", "class org.openrewrite.xml.tree.Xml$Tag"));
          }),
          //language=xml
          xml(
                """
            <!-- TODO: Have fun -->
            <!-- TODO: Test your code -->
            <xml>
                <is>too</is>
                <!-- TODO: Learn -->
                <verbose>for me</verbose>
            <!-- Just a regular comment -->
            </xml>
            """
          ),
          //language=markdown
          text(
            doesNotExist(),
            """
              ## Test Header
              TODO: Have fun
              TODO: Test your code
              TODO: Learn
              """,
            spec -> spec.path(Path.of("TODO.md")
            )
          )
        );
    }

    @Test
    void editExistingTodoFileXML() {
        // When the file does already exist, we assert the content is modified as expected.
        rewriteRun(
          spec -> spec.dataTable(TodoCommentsReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new TodoCommentsReport.Row("file.xml", "TODO: Have fun", "class org.openrewrite.xml.tree.Xml$Prolog"),
                new TodoCommentsReport.Row("file.xml", "TODO: Test your code", "class org.openrewrite.xml.tree.Xml$Prolog"),
                new TodoCommentsReport.Row("file.xml", "TODO: Learn", "class org.openrewrite.xml.tree.Xml$Tag"));
          }),
          //language=xml
          xml(
                """
            <!-- TODO: Have fun -->
            <!-- TODO: Test your code -->
            <xml>
                <is>too</is>
                <!-- TODO: Learn -->
                <verbose>for me</verbose>
            <!-- Just a regular comment -->
            </xml>
            """
          ),
          //language=markdown
          text(
            "",
            """
              ## Test Header
              TODO: Have fun
              TODO: Test your code
              TODO: Learn
              """,
            spec -> spec.path(Path.of("TODO.md")
            )
          )
        );
    }
}

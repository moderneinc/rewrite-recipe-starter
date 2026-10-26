/*
 * Copyright 2026 the original author or authors.
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

import io.moderne.devcenter.table.UpgradesAndMigrations;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class GuavaListsMigrationTest implements RewriteTest {

    // The card reads the source set marker to decide whether a repository depends on Guava
    private static final Consumer<SourceSpec<J.CompilationUnit>> GUAVA_ON_CLASSPATH =
      spec -> spec.markers(JavaSourceSet.build("main", JavaParser.dependenciesFromClasspath("guava")));

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new GuavaListsMigration())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @Test
    void callsNewArrayList() {
        rewriteRun(
          spec -> spec.dataTable(UpgradesAndMigrations.Row.class, rows -> assertThat(rows).containsExactly(
            new UpgradesAndMigrations.Row("Replace Guava Lists.newArrayList", 0, "Guava `Lists`", "Guava `Lists`"))),
          //language=java
          java(
            """
              import com.google.common.collect.Lists;
              import java.util.List;

              class A {
                  List<String> names = Lists.newArrayList();
              }
              """,
            GUAVA_ON_CLASSPATH
          )
        );
    }

    @Test
    void completed() {
        rewriteRun(
          spec -> spec.dataTable(UpgradesAndMigrations.Row.class, rows -> assertThat(rows).containsExactly(
            new UpgradesAndMigrations.Row("Replace Guava Lists.newArrayList", 1, "Completed", "Completed"))),
          //language=java
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class A {
                  List<String> names = new ArrayList<>();
              }
              """,
            GUAVA_ON_CLASSPATH
          )
        );
    }

    @Test
    void repositoryReportsItsFurthestFileFromCompleted() {
        rewriteRun(
          spec -> spec.dataTable(UpgradesAndMigrations.Row.class, rows -> assertThat(rows).containsExactly(
            new UpgradesAndMigrations.Row("Replace Guava Lists.newArrayList", 0, "Guava `Lists`", "Guava `Lists`"))),
          //language=java
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class A {
                  List<String> names = new ArrayList<>();
              }
              """,
            GUAVA_ON_CLASSPATH
          ),
          //language=java
          java(
            """
              import com.google.common.collect.Lists;
              import java.util.List;

              class B {
                  List<String> names = Lists.newArrayListWithCapacity(10);
              }
              """,
            GUAVA_ON_CLASSPATH
          )
        );
    }

    @Test
    void noRowWithoutGuava() {
        rewriteRun(
          spec -> spec.afterRecipe(run -> assertThat(run.getDataTableStore().getDataTables())
            .noneMatch(dataTable -> dataTable.getType().equals(UpgradesAndMigrations.Row.class))),
          //language=java
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class A {
                  List<String> names = new ArrayList<>();
              }
              """
          )
        );
    }
}

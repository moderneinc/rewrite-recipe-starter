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

import com.yourorg.table.ClassHierarchyReport;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class ClassHierarchyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ClassHierarchy());
    }

    @Test
    void basic() {
        rewriteRun(
          spec -> spec.dataTable(ClassHierarchyReport.Row.class, rows -> {
              assertThat(rows).containsExactly(new ClassHierarchyReport.Row("A", ClassHierarchyReport.Relationship.EXTENDS, "java.lang.Object"));
          }),
          //language=java
          java(
            """
              class A {}
              """
          )
        );
    }

    @Test
    void bExtendsA() {
        rewriteRun(
          spec -> spec.dataTable(ClassHierarchyReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new ClassHierarchyReport.Row("A", ClassHierarchyReport.Relationship.EXTENDS, "java.lang.Object"),
                new ClassHierarchyReport.Row("B", ClassHierarchyReport.Relationship.EXTENDS, "A"));
          }),
          //language=java
          java(
            """
              class A {}
              """
          ),
          //language=java
          java(
            """
              class B extends A {}
              """
          )
        );
    }

    @Test
    void interfaceRelationship() {
        rewriteRun(
          spec -> spec.dataTable(ClassHierarchyReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new ClassHierarchyReport.Row("A", ClassHierarchyReport.Relationship.EXTENDS, "java.lang.Object"),
                new ClassHierarchyReport.Row("A", ClassHierarchyReport.Relationship.IMPLEMENTS, "java.io.Serializable"));
          }),
          // language=java
          java(
            """
              import java.io.Serializable;
              class A implements Serializable {}
              """
          )
        );
    }
}

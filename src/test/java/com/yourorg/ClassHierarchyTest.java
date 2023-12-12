package com.yourorg;

import com.yourorg.table.ClassHierarchyReport;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.Assertions;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

public class ClassHierarchyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ClassHierarchy());
    }

    @Test
    void basic() {
        rewriteRun(
          spec -> spec.dataTable(ClassHierarchyReport.Row.class, rows -> {
              assertThat(rows).containsExactly(new ClassHierarchyReport.Row("A", ClassHierarchyReport.Relationship.EXTENDS,"java.lang.Object"));
          }),
          //language=java
          java("""
            class A {}
            """)
        );
    }

    @Test
    void aExtendsB() {
        rewriteRun(
          spec -> spec.dataTable(ClassHierarchyReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new ClassHierarchyReport.Row("B", ClassHierarchyReport.Relationship.EXTENDS,"java.lang.Object"),
                new ClassHierarchyReport.Row("A", ClassHierarchyReport.Relationship.EXTENDS,"B"));
          }),
          //language=java
          java("""
            class B {}
            """),
          java("""
            class A extends B {}
            """)
        );
    }

    @Test
    void interfaceRelationship() {
        rewriteRun(
          spec -> spec.dataTable(ClassHierarchyReport.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new ClassHierarchyReport.Row("A", ClassHierarchyReport.Relationship.EXTENDS,"java.lang.Object"),
                new ClassHierarchyReport.Row("A", ClassHierarchyReport.Relationship.IMPLEMENTS,"java.io.Serializable"));
          }),
          // language=java
          java("""
            import java.io.Serializable;
            class A implements Serializable {}
            """)
        );
    }
}

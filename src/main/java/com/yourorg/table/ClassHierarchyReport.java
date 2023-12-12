package com.yourorg.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class ClassHierarchyReport extends DataTable<ClassHierarchyReport.Row> {

    public ClassHierarchyReport(Recipe recipe) {
        super(recipe,
                "Class hierarchy report",
                "Records inheritance relationships between classes.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Class name",
                description = "Fully qualified name of the class.")
        String className;

        @Column(displayName = "Relationship",
                description = "Whether the class implements a super interface or extends a superclass.")
        Relationship relationship;

        @Column(displayName = "Super class name",
                description = "Fully qualified name of the superclass.")
        String superClassName;
    }

    public enum Relationship {
        EXTENDS,
        IMPLEMENTS
    }
}

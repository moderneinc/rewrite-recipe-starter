package com.yourorg;

import com.yourorg.table.ClassHierarchyReport;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;


@Value
@EqualsAndHashCode(callSuper = true)
public class ClassHierarchy extends Recipe {

    transient ClassHierarchyReport report = new ClassHierarchyReport(this);

    @Override
    public String getDisplayName() {
        return "Class hierarchy";
    }

    @Override
    public String getDescription() {
        return "Produces a data table showing inheritance relationships between classes.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                JavaType.FullyQualified type = classDecl.getType();
                if(type instanceof JavaType.Class && type.getSupertype() != null) {
                    JavaType.FullyQualified supertype = type.getSupertype();
                    report.insertRow(ctx, new ClassHierarchyReport.Row(type.getFullyQualifiedName(),
                            ClassHierarchyReport.Relationship.EXTENDS,
                            supertype.getFullyQualifiedName()));

                    for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
                        report.insertRow(ctx, new ClassHierarchyReport.Row(
                                type.getFullyQualifiedName(),
                                ClassHierarchyReport.Relationship.IMPLEMENTS,
                                anInterface.getFullyQualifiedName()
                        ));
                    }
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }
}

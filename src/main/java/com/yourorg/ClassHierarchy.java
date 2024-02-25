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
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Value
@EqualsAndHashCode(callSuper = false)
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
                // Capture all classes, which all extend java.lang.Object
                if (type instanceof JavaType.Class && type.getSupertype() != null) {
                    JavaType.FullyQualified supertype = type.getSupertype();
                    // Capture the direct superclass
                    report.insertRow(ctx, new ClassHierarchyReport.Row(
                            type.getFullyQualifiedName(),
                            ClassHierarchyReport.Relationship.EXTENDS,
                            supertype.getFullyQualifiedName()));

                    // Capture all interfaces
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

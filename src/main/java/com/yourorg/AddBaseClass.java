package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddBaseClass extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add `extends` to a class";
    }

    @Override
    public String getDescription() {
        return "Add `extends` to a class.";
    }

    @Option(displayName = "Fully qualified class name",
            description = "A fully-qualified class name to be extended with.",
            example = "com.yourorg.MyBaseClass")
    String fullyQualifiedClassName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
                                                            ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (cd.getExtends() == null) {
                    cd = cd.withName(cd.getName().withSimpleName(cd.getName().getSimpleName() + " "));
                    cd = cd.getPadding().withExtends(JLeftPadded.build(TypeTree.build(fullyQualifiedClassName).withPrefix(Space.SINGLE_SPACE)));
                }
                return cd;
            }
        };
    }
}
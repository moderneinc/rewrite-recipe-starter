package com.yourorg;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class UseIntegerValueOf extends Recipe {

    public static final String INTEGER = "java.lang.Integer";

    @Override
    public String getDisplayName() {
        return "Use Integer.valueOf(x) instead of new Integer(x)";
    }

    @Override
    public String getDescription() {
        return "Replaces unnecessary boxing constructor calls with the more efficient Integer.valueOf(x).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>(INTEGER, false),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J update = super.visitNewClass(newClass, ctx);
                        if (update instanceof J.NewClass) {
                            J.NewClass nc = (J.NewClass) update;
                            if (TypeUtils.isOfClassType(nc.getType(), INTEGER)) {
                                Expression arg = nc.getArguments().get(0);
                                return JavaTemplate.builder("Integer.valueOf(#{any()})")
                                        .contextSensitive()
                                        .build()
                                        .apply(getCursor(), nc.getCoordinates().replace(), arg);
                            }
                            return nc;
                        }
                        return update;
                    }
                }
        );
    }
}

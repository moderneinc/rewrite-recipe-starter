package com.yourorg;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;

public class ReplaceMapOfWithUnmodifiableFixedOrderMap extends Recipe {

    private static final MethodMatcher MATCHER = new MethodMatcher("java.util.Map of(..)");

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Use `UnMofifiableFixedOrderMap` instead of `Map.of()` or `Map.ofEntries()`";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "UnMofifiableFixedOrderMap gives fixed order of iteration on successive test runs which prevents" +
                " randomness during regression using Snapshot-testing.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesMethod<>(MATCHER)),
                new JavaVisitor<ExecutionContext>() {

                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {

                        // Ensure arguments are in key-value pairs
                        List<Expression> arguments = method.getArguments();
                        if (arguments.size() % 2 != 0) {
                            return method; // Not key-value pairs, skip
                        }

                        // Add required imports
                        maybeAddImport("com.phonepe.payments.paymentservice.util.UnmodifiableFixedOrderMap");
                        maybeRemoveImport("java.util.Map");

                        // Build the replacement template
                        StringBuilder builderTemplate = new StringBuilder("UnmodifiableFixedOrderMap.<String, Boolean>builder()");
                        for (int i = 0; i < arguments.size(); i += 2) {
                            builderTemplate.append(".put(\"").append(arguments.get(i)).append("\", ").append(arguments.get(i+1)).append(")");
                        }
                        builderTemplate.append(".build()");

                        JavaTemplate template = JavaTemplate.builder(builderTemplate.toString())
                                .imports("com.phonepe.payments.paymentservice.util.UnmodifiableFixedOrderMap")
                                .build();

                        return template.apply(getCursor(), method.getCoordinates().replace());

                    }
                });
    }
}

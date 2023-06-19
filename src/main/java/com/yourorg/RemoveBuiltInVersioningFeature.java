package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.Collections;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveBuiltInVersioningFeature extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove the Shelter Release Plugin `builtInVersioningEnabled` feature flag";
    }

    @Override
    public String getDescription() {
        return "Remove the Shelter Release Plugin `builtInVersioningEnabled` feature flag.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("SomeType", true);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
                J.Assignment a = super.visitAssignment(assignment, executionContext);
                if (a.getVariable() instanceof J.FieldAccess) {
                    J.FieldAccess field = (J.FieldAccess) a.getVariable();
                    if (field.getTarget() instanceof J.FieldAccess
                        && ((J.FieldAccess) field.getTarget()).getSimpleName().equals("versioning")
                        && ((J.FieldAccess) field.getTarget()).getTarget() instanceof J.Identifier
                        && ((J.Identifier) ((J.FieldAccess) field.getTarget()).getTarget()).getSimpleName().equals("release")
                        && field.getSimpleName().equals("builtInVersioningEnabled")) {
                        return null;
                    }
                }
                return a;
            }

            @Override
            public @Nullable J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);
                if (m.getSimpleName().equals("release") && m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Lambda) {
                    J.Lambda l = (J.Lambda) m.getArguments().get(0);
                    J.Block b = (J.Block) l.getBody();

                    b = b.withStatements(ListUtils.map(b.getStatements(), statement -> {
                        if (statement instanceof J.MethodInvocation) {
                            statement = maybeHandleVersioning((J.MethodInvocation) statement);
                        } else if (statement instanceof J.Return) {
                            if (((J.Return) statement).getExpression() instanceof J.MethodInvocation) {
                                J.Return returnStatement = (J.Return) statement;
                                J.MethodInvocation maybeUpdated = maybeHandleVersioning((J.MethodInvocation) returnStatement.getExpression());
                                if (maybeUpdated == null) {
                                    statement = null;
                                }
                            } else if (((J.Return) statement).getExpression() instanceof J.Assignment
                                       && ((J.Assignment) ((J.Return) statement).getExpression()).getVariable() instanceof J.FieldAccess
                                       && ((J.FieldAccess) ((J.Assignment) ((J.Return) statement).getExpression()).getVariable()).getTarget() instanceof J.Identifier
                                       && ((J.Identifier) ((J.FieldAccess) ((J.Assignment) ((J.Return) statement).getExpression()).getVariable()).getTarget()).getSimpleName().equals("versioning")
                                       && ((J.FieldAccess) ((J.Assignment) ((J.Return) statement).getExpression()).getVariable()).getSimpleName().equals("builtInVersioningEnabled")) {
                                statement = null;
                            }
                        }
                        return statement;
                    }));

                    if (b.getStatements().isEmpty()) {
                        m = null;
                    } else {
                        m = m.withArguments(Collections.singletonList(l.withBody(b)));
                    }
                } else if (m.getSimpleName().equals("versioning") && m.getSelect() instanceof J.Identifier && ((J.Identifier) m.getSelect()).getSimpleName().equals("release")) {
                    m = maybeHandleVersioning(m);
                }

                return m;
            }

            private @Nullable J.MethodInvocation maybeHandleVersioning(J.MethodInvocation m) {
                if (!m.getSimpleName().equals("versioning")) {
                    return m;
                }

                if (m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Lambda) {
                    J.Lambda l = (J.Lambda) m.getArguments().get(0);
                    J.Block b = (J.Block) l.getBody();

                    b = b.withStatements(ListUtils.map(b.getStatements(), statement -> {
                        if (statement instanceof J.Assignment) {
                            statement = maybeHandleBuiltInVersioningFlag((J.Assignment) statement);
                        } else if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.Assignment) {
                            J.Assignment maybeUpdated = maybeHandleBuiltInVersioningFlag((J.Assignment) ((J.Return) statement).getExpression());
                            if (maybeUpdated == null) {
                                statement = null;
                            }
                        }
                        return statement;
                    }));

                    if (b.getStatements().isEmpty()) {
                        m = null;
                    } else {
                        m = m.withArguments(Collections.singletonList(l.withBody(b)));
                    }
                }

                return m;
            }

            private J.Assignment maybeHandleBuiltInVersioningFlag(J.Assignment a) {
                if (a.getVariable() instanceof J.Identifier && ((J.Identifier) a.getVariable()).getSimpleName().equals("builtInVersioningEnabled")) {
                    return null;
                }
                return a;
            }
        };
    }
}

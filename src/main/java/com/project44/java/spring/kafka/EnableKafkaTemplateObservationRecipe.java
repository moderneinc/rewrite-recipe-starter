package com.project44.java.spring.kafka.kafka;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.VariableNameUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.singletonList;

public class EnableKafkaTemplateObservationRecipe extends Recipe {

    private static final String KAFKA_TEMPLATE = "org.springframework.kafka.core.KafkaTemplate";
    private static final MethodMatcher SET_OBSERVATION_MATCHER = new MethodMatcher(KAFKA_TEMPLATE + " setObservationEnabled(boolean)");

    @Override
    public String getDisplayName() {
        return "Enable observation for KafkaTemplate beans";
    }

    @Override
    public String getDescription() {
        return "Adds `setObservationEnabled(true)` to KafkaTemplate beans to enable observability.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                // Check if method returns KafkaTemplate
                if (!TypeUtils.isOfClassType(md.getReturnTypeExpression().getType(), KAFKA_TEMPLATE)) {
                    return md;
                }

                // Check if setObservationEnabled is already called
                if (hasObservationEnabledCall(md)) {
                    return md;
                }

                // Process the method body
                if (md.getBody() != null) {
                    final String methodName = md.getSimpleName();
                    J.MethodDeclaration finalMd = md;
                    md = md.withBody(md.getBody().withStatements(
                            ListUtils.flatMap(md.getBody().getStatements(), stmt -> {
                                if (stmt instanceof J.Return) {
                                    J.Return returnStmt = (J.Return) stmt;
                                    if (returnStmt.getExpression() instanceof J.NewClass) {
                                        // Case: return new KafkaTemplate<>(...)
                                        J.NewClass newClass = (J.NewClass) returnStmt.getExpression();
                                        if (TypeUtils.isOfClassType(newClass.getType(), KAFKA_TEMPLATE)) {
                                            // Split return statement into assignment, setter call, and return
                                            return transformDirectReturn(finalMd, newClass, ctx);
                                        }
                                    } else if (returnStmt.getExpression() instanceof J.Identifier) {
                                        // Add setter call before return
                                        return Arrays.asList(getSetter(returnStmt, ctx), returnStmt);
                                    }
                                }
                                return Collections.singletonList(stmt);
                            })
                    ));
                }

                return md;
            }

            private boolean hasObservationEnabledCall(J.MethodDeclaration method) {
                if (method.getBody() == null) {
                    return false;
                }

                return method.getBody().getStatements().stream().anyMatch(stmt -> stmt instanceof Expression && SET_OBSERVATION_MATCHER.matches((Expression) stmt));
            }

            private java.util.List<Statement> transformDirectReturn(J.MethodDeclaration method, J.NewClass newClass, ExecutionContext ctx) {
                String template = VariableNameUtils.generateVariableName("template", getCursor(), VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER);
                String templateString = String.format("%s %2$s = #{any()};\n%2$s.setObservationEnabled(true);\nreturn %2$s;", method.getReturnTypeExpression().toString(), template);
                J.MethodDeclaration methodDeclaration = JavaTemplate.builder(templateString)
                        .imports(KAFKA_TEMPLATE)
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-beans-5", "spring-context-5", "spring-kafka-3"))
                        .build()
                        .apply(getCursor(), method.getCoordinates().replaceBody(), newClass);

                return methodDeclaration.getBody() == null ? singletonList(newClass) : methodDeclaration.getBody().getStatements();
            }

            private J.MethodInvocation getSetter(J.Return returnStmt, ExecutionContext ctx) {
                J.Identifier identifier = (J.Identifier) returnStmt.getExpression();
                return JavaTemplate.builder("#{any()}.setObservationEnabled(true)")
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-beans-5", "spring-context-5", "spring-kafka-3"))
                        .build()
                        .apply(new Cursor(getCursor(), identifier), identifier.getCoordinates().replace(), identifier)
                        .withPrefix(returnStmt.getPrefix());
            }
        };
    }
}

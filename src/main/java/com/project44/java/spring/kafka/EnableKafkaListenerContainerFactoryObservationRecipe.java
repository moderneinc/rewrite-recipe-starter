/*
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package com.project44.java.spring.kafka.kafka;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class EnableKafkaListenerContainerFactoryObservationRecipe extends Recipe {

    private static final String LISTENER_CONTAINER_FACTORY = "org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory";
    private static final AnnotationMatcher BEAN_ANNOTATION_MATCHER = new AnnotationMatcher("@org.springframework.context.annotation.Bean");
    private static final MethodMatcher SET_OBSERVATION_MATCHER = new MethodMatcher("org.springframework.kafka.listener.ContainerProperties setObservationEnabled(boolean)");

    @Override
    public String getDisplayName() {
        return "Enable observation for ConcurrentKafkaListenerContainerFactory beans";
    }

    @Override
    public String getDescription() {
        return "Adds `factory.getContainerProperties().setObservationEnabled(true)` to ConcurrentKafkaListenerContainerFactory beans to enable observability.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                // Check if method returns ConcurrentKafkaListenerContainerFactory
                if (!TypeUtils.isOfClassType(md.getReturnTypeExpression().getType(), LISTENER_CONTAINER_FACTORY)) {
                    return md;
                }

                // Check if observation is already enabled
                if (hasObservationEnabledCall(md)) {
                    return md;
                }

                // Add observation enabled call before return statement
                if (md.getBody() != null) {
                    md = md.withBody(md.getBody().withStatements(
                            ListUtils.flatMap(md.getBody().getStatements(), stmt -> {
                                if (stmt instanceof J.Return) {
                                    J.Return returnStmt = (J.Return) stmt;
                                    if (returnStmt.getExpression() instanceof J.Identifier) {
                                        // Find the factory variable name
                                        J.Identifier factoryVar = (J.Identifier) returnStmt.getExpression();

                                        // Create the observation enabled call
                                        J.MethodInvocation observationCall = JavaTemplate.builder("#{any()}.getContainerProperties().setObservationEnabled(true)")
                                                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-beans-5", "spring-context-5", "spring-kafka-3"))
                                                .build()
                                                .apply(new Cursor(getCursor(), factoryVar), factoryVar.getCoordinates().replace(), factoryVar)
                                                .withPrefix(returnStmt.getPrefix());

                                        return asList(observationCall, returnStmt);
                                    }
                                }
                                return singletonList(stmt);
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
        };
    }
}

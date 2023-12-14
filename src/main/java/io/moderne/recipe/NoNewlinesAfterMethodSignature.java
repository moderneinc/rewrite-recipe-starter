package io.moderne.recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

public class NoNewlinesAfterMethodSignature extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove extra newlines after method signature";
    }

    @Override
    public String getDescription() {
        return "If there is an empty line after a method signature, remove it. The first statement of the method should have no empty lines before it.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration methodDeclaration = super.visitMethodDeclaration(method, executionContext);
                List<Statement> statements = new ArrayList<>();

                if (methodDeclaration.getBody() != null && methodDeclaration.getBody().getStatements().size() > 0) {
                    J firstStatement = methodDeclaration.getBody().getStatements().get(0);
                    if (!firstStatement.getPrefix().getWhitespace().contains("\n\n")) {
                        return methodDeclaration;
                    }
                    String newPrefix = firstStatement.getPrefix().getWhitespace().replaceAll("\n+", "\n");
                    statements.add(firstStatement.withPrefix(firstStatement.getPrefix().withWhitespace(newPrefix)));
                    statements.addAll(methodDeclaration.getBody().getStatements().subList(1, methodDeclaration.getBody().getStatements().size()));
                    return methodDeclaration.withBody(methodDeclaration.getBody().withStatements(statements));
                }

                return methodDeclaration;
            }
        };
    }
}

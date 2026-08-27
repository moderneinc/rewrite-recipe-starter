/*
 * Copyright 2025 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.analysis.dataflow.DataFlowNode;
import org.openrewrite.analysis.dataflow.DataFlowSpec;
import org.openrewrite.analysis.dataflow.FindLocalFlowPaths;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.IdentityHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Demonstrates data flow analysis with {@link DataFlowSpec} used to <em>prove a transformation safe</em> rather than
 * to search. Swapping the synchronized {@code StringBuffer} for {@code StringBuilder} is only sound when the instance
 * never leaves the method that created it, which no amount of pattern matching can establish. Here the source of the
 * flow is the {@code new StringBuffer(...)} initializer and the sinks are the places the value would escape; the
 * rewrite happens only when {@link FindLocalFlowPaths#noneMatch} proves no such path exists.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UseStringBuilderForLocalStringBuffer extends Recipe {

    private static final String STRING_BUFFER = "java.lang.StringBuffer";
    private static final String STRING_BUILDER = "java.lang.StringBuilder";

    String displayName = "Use `StringBuilder` for `StringBuffer` that never escapes its method";

    String description = "Replaces a local `StringBuffer` with the unsynchronized `StringBuilder` when data flow " +
                         "analysis proves the instance never escapes the method that creates it. A `StringBuffer` " +
                         "that is returned, assigned to a field, or passed to another method may be shared across " +
                         "threads and is left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(STRING_BUFFER, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);

                Expression initializer = v.getInitializer();
                JavaType.Variable variableType = v.getVariableType();
                if (variableType == null || !(variableType.getOwner() instanceof JavaType.Method) ||
                    !(initializer instanceof J.NewClass) || !TypeUtils.isOfClassType(initializer.getType(), STRING_BUFFER)) {
                    return v;
                }

                // A type expression shared by multiple variables cannot be changed for just one of them.
                J.VariableDeclarations declaration = getCursor().firstEnclosing(J.VariableDeclarations.class);
                if (declaration != null && declaration.getVariables().size() > 1) {
                    return v;
                }

                DataFlowSpec escapes = new DataFlowSpec() {
                    @Override
                    public boolean isSource(DataFlowNode srcNode) {
                        return initializer == srcNode.getCursor().getValue();
                    }

                    @Override
                    public boolean isSink(DataFlowNode sinkNode) {
                        return escapesMethod(sinkNode.getCursor());
                    }
                };
                if (!FindLocalFlowPaths.noneMatch(getCursor(), escapes)) {
                    return v;
                }

                Expression newInitializer = (Expression) new ChangeType(STRING_BUFFER, STRING_BUILDER, false)
                        .getVisitor().visitNonNull(initializer, ctx, getCursor().getParentOrThrow());
                JavaType newType = requireNonNull(newInitializer.getType());
                JavaType.Variable newVariableType = variableType.withType(newType);
                v = v.withInitializer(newInitializer)
                        .withVariableType(newVariableType)
                        .withName(v.getName().withType(newType).withFieldType(newVariableType));

                // Uses of the variable elsewhere still point at the old JavaType.Variable, so register the
                // replacement for visitIdentifier to pick up as the rest of the method is visited.
                Map<JavaType.Variable, JavaType.Variable> retype = getCursor().getNearestMessage("retype");
                if (retype == null) {
                    retype = new IdentityHashMap<>();
                    getCursor().putMessageOnFirstEnclosing(J.CompilationUnit.class, "retype", retype);
                }
                retype.put(variableType, newVariableType);
                getCursor().putMessageOnFirstEnclosing(J.VariableDeclarations.class, "replace", true);
                return v;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);
                if (getCursor().getMessage("replace", false)) {
                    mv = mv.withTypeExpression((TypeTree) new ChangeType(STRING_BUFFER, STRING_BUILDER, false)
                            .getVisitor().visit(mv.getTypeExpression(), ctx, getCursor().getParentOrThrow()));
                }
                return mv;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                Map<JavaType.Variable, JavaType.Variable> retype = getCursor().getNearestMessage("retype");
                if (retype != null && identifier.getFieldType() != null && retype.containsKey(identifier.getFieldType())) {
                    JavaType.Variable newVariableType = retype.get(identifier.getFieldType());
                    return identifier.withType(newVariableType.getType()).withFieldType(newVariableType);
                }
                return identifier;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                JavaType.Method methodType = mi.getMethodType();
                // Once the receiver has been re-typed, its still-`StringBuffer` method type (and, for chained
                // calls like `append(..).append(..)`, its return type) must follow.
                if (methodType != null && mi.getSelect() != null &&
                    TypeUtils.isOfClassType(mi.getSelect().getType(), STRING_BUILDER) &&
                    TypeUtils.isOfClassType(methodType.getDeclaringType(), STRING_BUFFER)) {
                    JavaType.Method newMethodType = remapMethodType(methodType);
                    mi = mi.withMethodType(newMethodType).withName(mi.getName().withType(newMethodType));
                }
                return mi;
            }
        });
    }

    /**
     * Contexts in which the value leaves the method, or in which its static type is relied upon in a way
     * `StringBuilder` may not satisfy (a cast, an array element, either branch of a ternary).
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    private static boolean escapesMethod(Cursor cursor) {
        Object value = cursor.getValue();
        Object parent = cursor.getParentTreeCursor().getValue();
        if (parent instanceof J.Return || parent instanceof J.TypeCast ||
            parent instanceof J.NewArray || parent instanceof J.Ternary) {
            return true;
        }
        if (parent instanceof J.MethodInvocation) {
            return ((J.MethodInvocation) parent).getArguments().contains(value);
        }
        if (parent instanceof J.NewClass) {
            return ((J.NewClass) parent).getArguments().contains(value);
        }
        if (parent instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) parent;
            Expression target = assignment.getVariable();
            return assignment.getAssignment() == value &&
                   (target instanceof J.FieldAccess ||
                    (target instanceof J.Identifier && ((J.Identifier) target).getFieldType() != null));
        }
        if (parent instanceof J.VariableDeclarations.NamedVariable) {
            // Aliased into another local declared as `StringBuffer`, whose declared type we are not changing.
            J.VariableDeclarations.NamedVariable target = (J.VariableDeclarations.NamedVariable) parent;
            return value instanceof J.Identifier && target.getInitializer() == value &&
                   TypeUtils.isOfClassType(target.getType(), STRING_BUFFER);
        }
        return false;
    }

    private static JavaType.Method remapMethodType(JavaType.Method methodType) {
        return methodType
                .withDeclaringType((JavaType.FullyQualified) remapType(methodType.getDeclaringType()))
                .withReturnType(remapType(methodType.getReturnType()))
                .withParameterTypes(ListUtils.map(methodType.getParameterTypes(), UseStringBuilderForLocalStringBuffer::remapType));
    }

    private static JavaType remapType(JavaType type) {
        if (!TypeUtils.isOfClassType(type, STRING_BUFFER)) {
            return type;
        }
        JavaType.FullyQualified replacement = TypeUtils.asFullyQualified(JavaType.buildType(STRING_BUILDER));
        return replacement == null ? type : replacement;
    }
}

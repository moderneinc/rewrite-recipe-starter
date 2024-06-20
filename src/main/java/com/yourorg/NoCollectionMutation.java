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

import fj.data.Option;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.analysis.dataflow.DataFlowNode;
import org.openrewrite.analysis.dataflow.DataFlowSpec;
import org.openrewrite.analysis.dataflow.Dataflow;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class NoCollectionMutation extends Recipe {
    @Override
    public String getDisplayName() {
        return "Prevent LST collection mutation";
    }

    @Override
    public String getDescription() {
        return "LST elements should always be treated as immutable, even for fields that are not protected from mutation at runtime. " +
               "Adding or removing an element from a collection on an LST element is always a bug. " +
               "This recipe uses Dataflow analysis to detect and put defensive copies around collection mutations.";
    }

    private static final MethodMatcher ADD_MATCHER = new MethodMatcher("java.util.List add(..)");
    private static final MethodMatcher ADD_ALL_MATCHER = new MethodMatcher("java.util.List addAll(..)");
    private static final MethodMatcher CLEAR_MATCHER = new MethodMatcher("java.util.List clear()");
    private static final MethodMatcher REMOVE_MATCHER = new MethodMatcher("java.util.List remove(..)");
    private static final MethodMatcher REMOVE_ALL_MATCHER = new MethodMatcher("java.util.List removeAll(..)");
    private static final MethodMatcher REPLACE_MATCHER = new MethodMatcher("java.util.List replace(..)");
    private static final MethodMatcher SET_MATCHER = new MethodMatcher("java.util.List set(..)");
    private static final MethodMatcher SORT_MATCHER = new MethodMatcher("java.util.List sort(..)");
    /**
     * The "select" of a method is the receiver or target of the invocation. In the method call "aList.add(foo)" the "select" is "aList".
     *
     * @param cursor a stack of LST elements with parent/child relationships connecting an individual LST element to the root of the tree
     * @return true if the cursor points to the "select" of a method invocation that is a list mutation
     */
    private static boolean isListMutationSelect(Cursor cursor) {
        Object parentValue = cursor.getParentTreeCursor().getValue();
        if (!(parentValue instanceof J.MethodInvocation)
            || ((J.MethodInvocation) parentValue).getMethodType() == null
            || ((J.MethodInvocation) parentValue).getSelect() != cursor.getValue()) {
            return false;
        }
        JavaType.Method mt = ((J.MethodInvocation) parentValue).getMethodType();
        return ADD_MATCHER.matches(mt) ||
               ADD_ALL_MATCHER.matches(mt) ||
               CLEAR_MATCHER.matches(mt) ||
               REMOVE_MATCHER.matches(mt) ||
               REMOVE_ALL_MATCHER.matches(mt) ||
               REPLACE_MATCHER.matches(mt) ||
               SET_MATCHER.matches(mt) ||
               SORT_MATCHER.matches(mt);
    }

    private static final MethodMatcher NEW_ARRAY_LIST_MATCHER = new MethodMatcher("java.util.ArrayList <init>(java.util.Collection)");

    /**
     * @param cursor a stack of LST elements with parent/child relationships connecting an individual LST element to the root of the tree
     * @return true if the cursor points to an LST element contained within the argument list of a constructor or
     * function which creates a defensive copy as needed
     */
    private static boolean inDefensiveCopy(@Nullable Cursor cursor) {
        if(cursor == null) {
            return false;
        }
        Object value = cursor.getValue();
        if (value instanceof J.NewClass && NEW_ARRAY_LIST_MATCHER.matches(((J.NewClass) value).getMethodType())) {
            return true;
        }
        return inDefensiveCopy(cursor.getParent());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaVisitor<ExecutionContext> addDefensiveCopy = new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                if (m.getMethodType() == null || !(m.getMethodType().getDeclaringType() instanceof JavaType.Class)) {
                    return m;
                }
                JavaType.Method mt = m.getMethodType();
                JavaType.Class declaringType = (JavaType.Class) mt.getDeclaringType();
                if (!TypeUtils.isAssignableTo("org.openrewrite.Tree", declaringType) || !TypeUtils.isAssignableTo("java.util.List", mt.getReturnType())) {
                    return m;
                }

                boolean isMutated = Dataflow.startingAt(getCursor()).findSinks(new DataFlowSpec() {
                            @Override
                            public boolean isSource(DataFlowNode srcNode) {
                                return true;
                            }

                            @Override
                            public boolean isSink(DataFlowNode sinkNode) {
                                return isListMutationSelect(sinkNode.getCursor());
                            }
                        }).bind(sinkFlow -> {
                            for (Cursor sink : sinkFlow.getSinkCursors()) {
                                if(!inDefensiveCopy(sink)) {
                                    return Option.some(sink);
                                }
                            }
                            return Option.none();
                        })
                        .isSome();
                if(!isMutated) {
                    return m;
                }

                maybeAddImport("java.util.ArrayList");
                return JavaTemplate.builder("new ArrayList<>(#{any(java.util.List)})")
                        .imports("java.util.ArrayList")
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m);
            }
        };

        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>("org.openrewrite.Tree", true),
                        new UsesType<>("java.util.List", true)),
                addDefensiveCopy);
    }
}

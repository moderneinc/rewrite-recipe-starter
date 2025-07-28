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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class UseIntegerValueOf extends Recipe {

    private static final MethodMatcher INTEGER_CONSTRUCTOR = new MethodMatcher("java.lang.Integer#<init>(*)");

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Use `Integer.valueOf(x)` or `Integer.parseInt(x)` instead of `new Integer(x)`";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Replaces unnecessary boxing constructor calls with the more efficient `Integer.valueOf(x)` for `int` values, or `Integer.parseInt(x)` for `String` values.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>(INTEGER_CONSTRUCTOR),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J update = super.visitNewClass(newClass, ctx);
                        if (!(update instanceof J.NewClass)) {
                            return update;
                        }
                        J.NewClass nc = (J.NewClass) update;
                        if (!INTEGER_CONSTRUCTOR.matches(nc)) {
                            return nc;
                        }

                        Expression arg = nc.getArguments().get(0);
                        if (TypeUtils.isString(arg.getType())) {
                            return JavaTemplate.builder("Integer.parseInt(#{any(java.lang.String)})")
                                    .build()
                                    .apply(getCursor(), nc.getCoordinates().replace(), arg);
                        }
                        return JavaTemplate.builder("Integer.valueOf(#{any()})")
                                .build()
                                .apply(getCursor(), nc.getCoordinates().replace(), arg);
                    }
                }
        );
    }
}

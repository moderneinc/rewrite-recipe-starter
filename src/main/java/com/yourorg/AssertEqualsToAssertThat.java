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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class AssertEqualsToAssertThat extends Recipe {
    @Override
    public String getDisplayName() {
        // language=markdown
        return "JUnit `assertEquals()` to Assertj `assertThat()`";
    }

    @Override
    public String getDescription() {
        return "Use AssertJ assertThat instead of JUnit assertEquals().";
    }

    private static MethodMatcher MATCHER = new MethodMatcher("org.junit.jupiter.api.Assertions assertEquals(..)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.junit.jupiter.api.Assertions", null),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        if (!MATCHER.matches(m)) {
                            return m;
                        }
                        List<Expression> arguments = m.getArguments();
                        maybeAddImport("org.assertj.core.api.Assertions");
                        maybeRemoveImport("org.junit.jupiter.api.Assertions");
                        if (arguments.size() == 2) {
                            Expression expected = arguments.get(0);
                            Expression actual = arguments.get(1);

                            m = JavaTemplate.builder("Assertions.assertThat(#{any()}).isEqualTo(#{any()})")
                                    .imports("org.assertj.core.api.Assertions")
                                    .javaParser(JavaParser.fromJavaVersion()
                                            .classpath("assertj-core"))
                                    .build()
                                    .apply(getCursor(), m.getCoordinates().replace(), actual, expected);
                        } else if (arguments.size() == 3) {
                            Expression expected = arguments.get(0);
                            Expression actual = arguments.get(1);
                            Expression description = arguments.get(2);

                            m = JavaTemplate.builder("Assertions.assertThat(#{any()}).as(#{any()}).isEqualTo(#{any()})")
                                    .imports("org.assertj.core.api.Assertions")
                                    .javaParser(JavaParser.fromJavaVersion()
                                            .classpath("assertj-core"))
                                    .build()
                                    .apply(getCursor(), m.getCoordinates().replace(), actual, description, expected);
                        }
                        return m;
                    }
                });
    }
}

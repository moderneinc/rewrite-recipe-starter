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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


@SuppressWarnings({"NullableProblems", "WriteOnlyObject", "ResultOfMethodCallIgnored", "DataFlowIssue"})
class NoCollectionMutationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoCollectionMutation()).parser(JavaParser.fromJavaVersion().classpath("rewrite-core", "rewrite-java"));
    }

    @Test
    void nonMutationIsOkay() {
        rewriteRun(
          //language=java
          java(
            """
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.internal.ListUtils;
              
              public class ManipulateMethodArguments extends JavaIsoVisitor<ExecutionContext> {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      method.getArguments().isEmpty();
                      method.getSideEffects().indexOf(null);
                      method.getTypeParameters().toArray();
                      return method;
                  }
              }
              """)
        );
    }

    @DocumentExample
    @Test
    void inlineMutation() {
        rewriteRun(
          //language=java
          java(
            """
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.J;
              
              public class ManipulateMethodArguments extends JavaIsoVisitor<ExecutionContext> {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      method.getArguments().clear();
                      return method;
                  }
              }
              """,
            """
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.J;
              
              import java.util.ArrayList;
              
              public class ManipulateMethodArguments extends JavaIsoVisitor<ExecutionContext> {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      new ArrayList<>(method.getArguments()).clear();
                      return method;
                  }
              }
              """)
        );
    }

    @Test
    void subsequentMutation() {
        rewriteRun(
          //language=java
          java(
            """
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              
              import java.util.List;
              
              public class ManipulateMethodArguments extends JavaIsoVisitor<ExecutionContext> {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      List<Expression> args = method.getArguments();
                      if(!args.isEmpty()) {
                          args.remove(0);
                      }
                      return method;
                  }
              }
              """,
            """
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              
              import java.util.ArrayList;
              import java.util.List;
              
              public class ManipulateMethodArguments extends JavaIsoVisitor<ExecutionContext> {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      List<Expression> args = new ArrayList<>(method.getArguments());
                      if(!args.isEmpty()) {
                          args.remove(0);
                      }
                      return method;
                  }
              }
              """)
        );
    }

    @Disabled("Local dataflow is not capable of following what happens to a variable passed into a function")
    @Test
    void mutationInFunction() {
        rewriteRun(
          //language=java
          java(
            """
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              
              import java.util.List;
              
              public class ManipulateMethodArguments extends JavaIsoVisitor<ExecutionContext> {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      List<Expression> a = method.getArguments();
                      removeFirst(a);
                      return method;
                  }
              
                  private void removeFirst(List<Expression> args) {
                      if(!args.isEmpty()) {
                          args.remove(0);
                      }
                  }
              }
              """,
            """
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              
              import java.util.ArrayList;
              import java.util.List;
              
              public class ManipulateMethodArguments extends JavaIsoVisitor<ExecutionContext> {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      List<Expression> a = new ArrayList<>(method.getArguments());
                      removeFirst(a);
                      return method;
                  }
              
                  private void removeFirst(List<Expression> args) {
                      if(!args.isEmpty()) {
                          args.remove(0);
                      }
                  }
              }
              """)
        );
    }

    @Test
    void listUtilsIsOkay() {
        rewriteRun(
          //language=java
          java(
            """
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.internal.ListUtils;
              
              public class ManipulateMethodArguments extends JavaIsoVisitor<ExecutionContext> {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                      ListUtils.map(method.getArguments(), it -> it).clear();
                      return method;
                  }
              }
              """)
        );
    }
}

/*
 * Copyright 2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.cleanup.ModifierOrder;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

@Value
@EqualsAndHashCode(callSuper = true)
public class StaticizeNonOverridableMethods extends Recipe {

  @Override
  public String getDisplayName() {
    return "Add `static` modifiers to private or final methods that don't access instance data";
  }

  @Override
  public String getDescription() {
    return "Add `static` modifiers to non-overridable methods (private or final) that don’t access instance data.";
  }

  @Override
  public JavaIsoVisitor<ExecutionContext> getVisitor() {
    return new JavaIsoVisitor<ExecutionContext>() {
      private final Set<J.VariableDeclarations.NamedVariable> instanceVariables = new HashSet<>();
      private final Set<J.MethodDeclaration> instanceMethods = new HashSet<>();

      @Override
      public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        // skip static class
        if (classDecl.hasModifier(J.Modifier.Type.Static)) {
          return classDecl;
        }

        instanceVariables.addAll(CollectInstanceVariables.collect(classDecl));
        instanceMethods.addAll(CollectInstanceMethods.collect(classDecl));

        return super.visitClassDeclaration(classDecl, ctx);
      }

      @Override
      public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

        // if it already has `static` modifier, it doesn't need to add again.
        if (m.hasModifier(J.Modifier.Type.Static)) {
          return m;
        }

        // if it's an overridable methods (neither `private` nor `final`), we don't want to add `static`.
        if (!m.hasModifier(J.Modifier.Type.Private) && !m.hasModifier(J.Modifier.Type.Final)) {
          return m;
        }

        boolean hasInstanceDataAccess = FindInstanceDataAccess.find(method,
            instanceMethods,
            instanceVariables
        ).get();

        return hasInstanceDataAccess ? m : addStatic(m);
      }
    };
  }

  private static J.MethodDeclaration addStatic(J.MethodDeclaration m) {
    // it expects the input method is non-overridable, so it should has at least one modifier(`private` or `final`)
    if (m.getModifiers().isEmpty()) {
      return m;
    }

    Space singleSpace = Space.build(" ", Collections.emptyList());
    J.Modifier staticMod = m.getModifiers().stream()
        .findFirst()
        .get()
        .withId(Tree.randomId())
        .withPrefix(singleSpace)
        .withType(J.Modifier.Type.Static);
    List<J.Modifier> modifiers = new ArrayList<>(m.getModifiers());
    modifiers.add(staticMod);
    return m.withModifiers(ModifierOrder.sortModifiers(modifiers));
  }

  @EqualsAndHashCode(callSuper = true)
  private static class FindInstanceDataAccess extends JavaIsoVisitor<AtomicBoolean> {
    private final J.Identifier currentMethod;
    private final Set<J.MethodDeclaration> instanceMethods;
    private final Set<J.VariableDeclarations.NamedVariable> instanceVariables;

    private FindInstanceDataAccess(J.Identifier currentMethod,
        Set<J.MethodDeclaration> instanceMethods,
        Set<J.VariableDeclarations.NamedVariable> instanceVariables
        ) {
      this.currentMethod = currentMethod;
      this.instanceMethods = instanceMethods;
      this.instanceVariables = instanceVariables;
    }

    /**
     * @param method the method to search for.
     * @return whether has instance data access in this method
     */
    static AtomicBoolean find(J.MethodDeclaration method,
        Set<J.MethodDeclaration> instanceMethods,
        Set<J.VariableDeclarations.NamedVariable> instanceVariables
    ) {
      return new FindInstanceDataAccess(method.getName(), instanceMethods, instanceVariables)
          .reduce(method, new AtomicBoolean());
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean hasInstanceDataAccess) {
      if (hasInstanceDataAccess.get()) {
        return identifier;
      }

      J.Identifier id = super.visitIdentifier(identifier, hasInstanceDataAccess);

      // Skip the method itself identifier.
      // UUID check is necessary to make sure it's the identifier of the method we are checking here.
      // to cover the scenario that the variable and method having the same name.
      if (id.getSimpleName().equals(currentMethod.getSimpleName())
          && id.getId().equals(currentMethod.getId())
          && id.getType() == null
          && id.getFieldType() == null
      ) {
        return id;
      }

      // todo, kunli, consider J.Identifier.getFieldType()

      // Do name matching here, since the loose checking conditions here makes lower false rewrites.
      boolean isInstanceVariable = instanceVariables.stream()
          .anyMatch(v -> v.getSimpleName().equals(id.getSimpleName()));

      boolean isInstanceMethod = instanceMethods.stream()
          .anyMatch(m -> m.getName().getSimpleName().equals(id.getSimpleName()));

      if (isInstanceVariable || isInstanceMethod) {
        hasInstanceDataAccess.set(true);
      }

      return id;
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  private static class CollectInstanceVariables extends JavaIsoVisitor<Set<J.VariableDeclarations.NamedVariable>> {
    /**
     * @param classDecl The target class to collect for
     * @return a set of instance variables
     */
    static Set<J.VariableDeclarations.NamedVariable> collect(J.ClassDeclaration classDecl) {
      return new CollectInstanceVariables()
          .reduce(classDecl, new HashSet<>());
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
        Set<J.VariableDeclarations.NamedVariable> instanceVariables
    ) {
      J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, instanceVariables);

      // skip class variables
      if (mv.hasModifier(J.Modifier.Type.Static)) {
        return mv;
      }

      instanceVariables.addAll(multiVariable.getVariables());
      return mv;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
        Set<J.VariableDeclarations.NamedVariable> instanceVariables
    ) {
      // skip method sub-tree traversal
      return method;
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  private static class CollectInstanceMethods extends JavaIsoVisitor<Set<J.MethodDeclaration>> {
    /**
     * @param classDecl The cursor of class declaration
     * @return a set of instance methods
     */
    static Set<J.MethodDeclaration> collect(J.ClassDeclaration classDecl) {
      return new CollectInstanceMethods()
          .reduce(classDecl, new HashSet<>());
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
        Set<J.MethodDeclaration> instanceMethods
    ) {
      // skip class methods
      if (method.hasModifier(J.Modifier.Type.Static)) {
        return method;
      }

      instanceMethods.add(method);
      return method;
    }
  }
}

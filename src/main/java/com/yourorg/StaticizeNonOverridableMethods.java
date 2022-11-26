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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.cleanup.ModifierOrder;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

@Value
@EqualsAndHashCode(callSuper = true)
public class StaticizeNonOverridableMethods extends Recipe {

  @Override
  public String getDisplayName() {
    return "Staticize non-overridable methods";
  }

  @Override
  public String getDescription() {
    return "Add `static` modifiers to non-overridable methods (private or final) that don’t access instance data.";
  }

  @Override
  public JavaIsoVisitor<ExecutionContext> getVisitor() {
    return new JavaIsoVisitor<ExecutionContext>() {
      final List<J.MethodDeclaration> staticInstanceMethods = new ArrayList<>();

      @Override
      public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        staticInstanceMethods.addAll(FindStaticInstanceMethods.find(getCursor().getValue()));
        return super.visitClassDeclaration(classDecl, ctx);
      }

      @Override
      public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        boolean canBeStatic = staticInstanceMethods.stream()
            .anyMatch(m -> method.getSimpleName().equals(m.getSimpleName()));
        return canBeStatic ? addStaticModifier(method) : method;
      }
    };
  }

  private static J.MethodDeclaration addStaticModifier(J.MethodDeclaration m) {
    List<J.Modifier> modifiers = m.getModifiers();

    // it expects the input method is non-overridable, so it should has at least one modifier(`private` or `final`)
    if (modifiers.isEmpty()) {
      return m;
    }

    boolean hasFinalModifier = modifiers.stream()
        .anyMatch(mod -> mod.getType() == J.Modifier.Type.Final);

    if (hasFinalModifier) {
      // replace `final` with `static`, since it's redundant.
      modifiers = ListUtils.map(m.getModifiers(),
          (i, mod) -> mod.getType() == J.Modifier.Type.Final ? mod.withType(J.Modifier.Type.Static) : mod);
    } else {
      // add `static` modifier
      modifiers = ListUtils.concat(modifiers, buildStaticModifier(modifiers));
    }

    return m.withModifiers(ModifierOrder.sortModifiers(modifiers));
  }

  private static J.Modifier buildStaticModifier(List<J.Modifier> ms) {
    // ms is guaranteed contains `private` or `final`.
    Space singleSpace = Space.build(" ", Collections.emptyList());
    return ms.stream()
        .filter(mod -> mod.getType() == J.Modifier.Type.Private || mod.getType() == J.Modifier.Type.Final)
        .findFirst()
        .get()
        .withId(Tree.randomId())
        .withPrefix(singleSpace)
        .withType(J.Modifier.Type.Static);
  }

  private static List<J.MethodDeclaration> getInstanceMethods(J.ClassDeclaration classDecl) {
    return classDecl.getBody()
        .getStatements()
        .stream()
        .filter(statement -> statement instanceof J.MethodDeclaration)
        .map(J.MethodDeclaration.class::cast)
        .filter(m -> !m.hasModifier(J.Modifier.Type.Static))
        .collect(Collectors.toList());
  }

  private static List<J.VariableDeclarations.NamedVariable> getInstanceVariables(J.ClassDeclaration classDecl) {
    return classDecl.getBody()
        .getStatements()
        .stream()
        .filter(statement -> statement instanceof J.VariableDeclarations)
        .map(J.VariableDeclarations.class::cast)
        .filter(mv -> !mv.hasModifier(J.Modifier.Type.Static))
        .map(J.VariableDeclarations::getVariables)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  /**
   * Visitor to find all non-overridable instance methods (private or final) which don't access instance data,
   * so they can be static.
   */
  @EqualsAndHashCode(callSuper = true)
  private static class FindStaticInstanceMethods extends JavaIsoVisitor<List<J.MethodDeclaration>> {
    private final List<J.MethodDeclaration> instanceMethods;
    private final List<J.VariableDeclarations.NamedVariable> instanceVariables;

    private FindStaticInstanceMethods() {
      this.instanceMethods = new ArrayList<>();
      this.instanceVariables = new ArrayList<>();
    }

    /**
     * @param j The subtree to search, supposed to be a class declaration cursor.
     * @return a list of instance methods that can be static.
     */
    static List<J.MethodDeclaration> find(J j) {
      return new FindStaticInstanceMethods()
          .reduce(j, new ArrayList<>());
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
        List<J.MethodDeclaration> staticInstanceMethods
    ) {
      // Skip nested class (inner class or static nested class)
      boolean isNestedClass = classDecl.getType() != null && classDecl.getType().getOwningClass() != null;
      if (isNestedClass) {
        return classDecl;
      }

      instanceVariables.addAll(getInstanceVariables(classDecl));
      instanceMethods.addAll(getInstanceMethods(classDecl));
      return super.visitClassDeclaration(classDecl, staticInstanceMethods);
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
        List<J.MethodDeclaration> staticInstanceMethods
    ) {
      J.MethodDeclaration m = super.visitMethodDeclaration(method, staticInstanceMethods);

      // if it already has `static` modifier, it doesn't need to add again.
      if (m.hasModifier(J.Modifier.Type.Static)) {
        return m;
      }

      // if it's an overridable methods (neither `private` nor `final`), we don't want to add `static`.
      if (!m.hasModifier(J.Modifier.Type.Private) && !m.hasModifier(J.Modifier.Type.Final)) {
        return m;
      }

      boolean hasInstanceDataAccess = FindInstanceDataAccess.find(getCursor().getValue(),
          instanceMethods,
          instanceVariables
      ).get();

      if (!hasInstanceDataAccess) {
        staticInstanceMethods.add(m);
      }
      return m;
    }
  }

  /**
   * Visitor to find instance data access in a method
   */
  @EqualsAndHashCode(callSuper = true)
  private static class FindInstanceDataAccess extends JavaIsoVisitor<AtomicBoolean> {
    private final List<J.MethodDeclaration> instanceMethods;
    private final List<J.VariableDeclarations.NamedVariable> instanceVariables;

    private FindInstanceDataAccess(List<J.MethodDeclaration> instanceMethods,
        List<J.VariableDeclarations.NamedVariable> instanceVariables
    ) {
      this.instanceMethods = instanceMethods;
      this.instanceVariables = instanceVariables;
    }

    /**
     * @param j The subtree to search.
     * @return whether has instance data access in this method
     */
    static AtomicBoolean find(J j,
        List<J.MethodDeclaration> instanceMethods,
        List<J.VariableDeclarations.NamedVariable> instanceVariables
    ) {
      return new FindInstanceDataAccess(instanceMethods, instanceVariables)
          .reduce(j, new AtomicBoolean());
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean hasInstanceDataAccess) {
      if (hasInstanceDataAccess.get()) {
        return identifier;
      }

      J.Identifier id = super.visitIdentifier(identifier, hasInstanceDataAccess);

      // instance method calls will be handled by `visitMethodInvocation`, handles instance variables only here.
      boolean isNotVariable = id.getType() == null || id.getFieldType() == null;
      if (isNotVariable) {
        return id;
      }

      boolean isInstanceVariable = instanceVariables.stream()
          .anyMatch(v -> id.getFieldType().equals(v.getName().getFieldType())
              && id.getType().equals(v.getName().getType())
              && id.getSimpleName().equals(v.getSimpleName()));

      if (isInstanceVariable) {
        hasInstanceDataAccess.set(true);
      }

      return id;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean hasInstanceDataAccess) {
      if (hasInstanceDataAccess.get()) {
        return method;
      }

      J.MethodInvocation m = super.visitMethodInvocation(method, hasInstanceDataAccess);

      boolean isInstanceMethod = instanceMethods.stream()
          .anyMatch(im -> im.getSimpleName().equals(method.getSimpleName()));

      if (isInstanceMethod) {
        hasInstanceDataAccess.set(true);
      }

      return m;
    }
  }
}

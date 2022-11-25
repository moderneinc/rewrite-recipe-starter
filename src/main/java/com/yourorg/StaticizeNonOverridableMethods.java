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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
      final Set<J.VariableDeclarations.NamedVariable> instanceVariables = new HashSet<>();
      final Set<J.MethodDeclaration> instanceMethods = new HashSet<>();

      @Override
      public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        System.out.println("[Kun] visitClassDeclaration : " + classDecl);
        // todo. kunli, handle inner class


        // skip static class
        if (classDecl.hasModifier(J.Modifier.Type.Static)) {
          return classDecl;
        }

        instanceVariables.addAll(CollectInstanceVariables.collect(getCursor().getValue()));
        instanceMethods.addAll(CollectInstanceMethods.collect(getCursor().getValue()));

        return super.visitClassDeclaration(classDecl, ctx);
      }

      @Override
      public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        System.out.println("[Kun] visitMethodDeclaration : " + method);

        // if it already has `static` modifier, it doesn't need to add again.
        if (method.hasModifier(J.Modifier.Type.Static)) {
          return method;
        }

        // if it's an overridable methods (neither `private` nor `final`), we don't want to add `static`.
        if (!method.hasModifier(J.Modifier.Type.Private) && !method.hasModifier(J.Modifier.Type.Final)) {
          return method;
        }

        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

        boolean hasInstanceDataAccess = FindInstanceDataAccess.find(getCursor().getValue(),
            method.getName(),
            instanceMethods,
            instanceVariables
        ).get();

        return hasInstanceDataAccess ? m : addStaticModifier(m);
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
    // ms is guaranteed not empty.
    Space singleSpace = Space.build(" ", Collections.emptyList());
    return ms.stream()
        .findFirst()
        .get()
        .withId(Tree.randomId())
        .withPrefix(singleSpace)
        .withType(J.Modifier.Type.Static);
  }

  /**
   * Visitor to find instance data access in a method
   */
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
     * @param j The subtree to search.
     * @return whether has instance data access in this method
     */
    static AtomicBoolean find(J j,
        J.Identifier currentMethod,
        Set<J.MethodDeclaration> instanceMethods,
        Set<J.VariableDeclarations.NamedVariable> instanceVariables
    ) {
      return new FindInstanceDataAccess(currentMethod, instanceMethods, instanceVariables)
          .reduce(j, new AtomicBoolean());
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean hasInstanceDataAccess) {
      // todo, kunli, remove log
      System.out.println("[Kun] FindInstanceDataAccess.visitIdentifier : " + identifier
          + " [type=" + identifier.getType() + "] [field type=" + identifier.getFieldType() + "]");

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
      // todo, kunli. remove log
      System.out.println("[Kun] FindInstanceDataAccess.visitMethodInvocation : " + method
          + " [type=" + method.getType() + "] [field type=" + method.getName().getFieldType() + "]");

      if (hasInstanceDataAccess.get()) {
        return method;
      }

      boolean isInstanceMethod = instanceMethods.stream()
          .anyMatch(im -> im.getSimpleName().equals(method.getSimpleName()));

      if (isInstanceMethod) {
        hasInstanceDataAccess.set(true);
      }

      // skip sub-elements traversal
      return method;
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  private static class CollectInstanceVariables extends JavaIsoVisitor<Set<J.VariableDeclarations.NamedVariable>> {

    /**
     * @param j The subtree to search
     * @return a set of instance variables
     */
    static Set<J.VariableDeclarations.NamedVariable> collect(J j) {
      return new CollectInstanceVariables()
          .reduce(j, new HashSet<>());
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
        Set<J.VariableDeclarations.NamedVariable> instanceVariables
    ) {
      // todo, kunli. remove log
      System.out.println("[Kun] CollectInstanceVariables.visitVariableDeclarations : " + multiVariable);

      J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, instanceVariables);

      // skip class variables
      if (mv.hasModifier(J.Modifier.Type.Static)) {
        return mv;
      }

      // filter NULL `type` and `fieldType`, (todo, remove this check if it's guaranteed not null)
      List<J.VariableDeclarations.NamedVariable> vs = multiVariable.getVariables()
          .stream()
          .filter(nv -> nv.getName().getType() != null && nv.getName().getFieldType() != null)
          .collect(Collectors.toList());

      instanceVariables.addAll(vs);
      return mv;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
        Set<J.VariableDeclarations.NamedVariable> instanceVariables
    ) {
      // todo, kunli. remove log
      System.out.println("[Kun] CollectInstanceVariables.visitMethodDeclaration : " + method);

      // collect instance variables only , so skip method sub-tree traversal
      return method;
    }
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  private static class CollectInstanceMethods extends JavaIsoVisitor<Set<J.MethodDeclaration>> {
    /**
     * @param j The subtree to search.
     * @return a set of instance methods
     */
    static Set<J.MethodDeclaration> collect(J j) {
      return new CollectInstanceMethods()
          .reduce(j, new HashSet<>());
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
        Set<J.MethodDeclaration> instanceMethods
    ) {
      // todo, kunli. remove log
      System.out.println("[Kun] CollectInstanceMethods.visitMethodDeclaration : " + method);

      // skip class methods
      if (method.hasModifier(J.Modifier.Type.Static)) {
        return method;
      }

      instanceMethods.add(method);
      return method;
    }
  }
}

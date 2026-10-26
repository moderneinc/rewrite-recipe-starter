/*
 * Copyright 2026 the original author or authors.
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

import io.moderne.devcenter.DevCenterMeasure;
import io.moderne.devcenter.UpgradeMigrationCard;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.List;

/**
 * A DevCenter card tracking progress on replacing Guava's {@code Lists.newArrayList} with {@code new ArrayList<>()}.
 * See devcenter.yml for how cards are composed into a DevCenter.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class GuavaListsMigration extends UpgradeMigrationCard {
    // The same calls NoGuavaListsNewArrayList replaces, so running the fix recipe completes the migration
    private static final MethodMatcher NEW_ARRAY_LIST = new MethodMatcher("com.google.common.collect.Lists newArrayList()");
    private static final MethodMatcher NEW_ARRAY_LIST_CAPACITY = new MethodMatcher("com.google.common.collect.Lists newArrayListWithCapacity(int)");
    private static final MethodMatcher NEW_ARRAY_LIST_ITERABLE = new MethodMatcher("com.google.common.collect.Lists newArrayList(java.lang.Iterable)");

    String displayName = "Replace Guava's `Lists.newArrayList`";

    String description = "Track progress on replacing Guava's `Lists.newArrayList` with `new ArrayList<>()`.";

    /**
     * Each state a repository can be in, ordered from furthest from the goal to the goal itself.
     * A repository reports the lowest ordinal any of its source files reached.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Measure implements DevCenterMeasure {
        GuavaLists("Guava `Lists`", "Calls Guava's `Lists.newArrayList`."),
        Completed("Completed", "Depends on Guava, but no longer calls `Lists.newArrayList`.");

        private final String name;
        private final String description;
    }

    // The card's name in the DevCenter, and the name of its rows in the UpgradesAndMigrations data table
    @Override
    public String getInstanceName() {
        return "Replace Guava Lists.newArrayList";
    }

    @Override
    public List<DevCenterMeasure> getMeasures() {
        return Arrays.asList(Measure.values());
    }

    // The recipe the DevCenter offers to run to move repositories toward Completed
    @Override
    public String getFixRecipeId() {
        return "com.yourorg.NoGuavaListsNewArrayList";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                // Repositories that never depended on Guava have nothing to migrate, so report no row
                // rather than counting them as Completed
                if (!dependsOnGuava(cu)) {
                    return cu;
                }

                // The methods a compilation unit calls are already indexed, so there's no need to visit its tree
                boolean callsNewArrayList = cu.getTypesInUse().getUsedMethods().stream()
                        .anyMatch(m -> NEW_ARRAY_LIST.matches(m) ||
                                       NEW_ARRAY_LIST_CAPACITY.matches(m) ||
                                       NEW_ARRAY_LIST_ITERABLE.matches(m));
                Measure measure = callsNewArrayList ? Measure.GuavaLists : Measure.Completed;
                upgradesAndMigrations.insertRow(ctx, GuavaListsMigration.this, measure, measure.getName());

                // This card only reads the tree, so return it unchanged
                return cu;
            }
        };
    }

    private static boolean dependsOnGuava(J.CompilationUnit cu) {
        return cu.getMarkers().findFirst(JavaSourceSet.class)
                .map(sourceSet -> sourceSet.getGavToTypes().keySet().stream()
                        .anyMatch(gav -> gav.startsWith("com.google.guava:guava:")))
                .orElse(false);
    }
}

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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Space;

// TODO - This is a placeholder for a recipe that uses data tables.
// Implement a recipe that finds any comments in Java source files that contain `TODO`, and add them to a data table.
// You're done when all of the tests in `TrackJavaTodosTest` pass.
// The Java LST element `org.openrewrite.java.tree.Space` carries java comments in Java source files.
@Value
@EqualsAndHashCode(callSuper = false)
public class TrackJavaTodos extends Recipe {


    @Override
    public String getDisplayName() {
        return "Export TODOs from Java comments";
    }

    @Override
    public String getDescription() {
        return "Export TODOs from Java source comments into a data table.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(@Nullable Space space, Space.Location loc, ExecutionContext ctx) {
                // TODO implement me to find comments with TODO and add them to a data table
                return super.visitSpace(space, loc, ctx);
            }
        };
    }
}

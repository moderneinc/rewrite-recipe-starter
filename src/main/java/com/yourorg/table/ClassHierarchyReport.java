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
package com.yourorg.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class ClassHierarchyReport extends DataTable<ClassHierarchyReport.Row> {

    public ClassHierarchyReport(Recipe recipe) {
        super(recipe,
                "Class hierarchy report",
                "Records inheritance relationships between classes.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Class name",
                description = "Fully qualified name of the class.")
        String className;

        @Column(displayName = "Relationship",
                description = "Whether the class implements a super interface or extends a superclass.")
        Relationship relationship;

        @Column(displayName = "Super class name",
                description = "Fully qualified name of the superclass.")
        String superClassName;
    }

    public enum Relationship {
        EXTENDS,
        IMPLEMENTS
    }
}

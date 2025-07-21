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

import org.openrewrite.Recipe;

// TODO - This is a placeholder for an imperative recipe.
// Implement a recipe that replaces all `new Integer(x)` constructors with Integer.valueOf(x) for int values, or Integer.parseInt(x) for String values.
// You're done when all of the tests in `UseIntegerValueOfTest` pass.
public class UseIntegerValueOf extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use Integer.valueOf(x) or Integer.parseInt(x) instead of new Integer(x)";
    }

    @Override
    public String getDescription() {
        return "Replaces unnecessary boxing constructor calls with the more efficient Integer.valueOf(x) for int values, or Integer.parseInt(x) for String values.";
    }
}

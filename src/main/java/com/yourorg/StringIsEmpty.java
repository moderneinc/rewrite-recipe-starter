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

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.openrewrite.java.template.RecipeDescriptor;

@RecipeDescriptor(
        //language=markdown
        name = "Standardize empty String checks",
        //language=markdown
        description = "Replace calls to `String.length() == 0` with `String.isEmpty()`."
)
public class StringIsEmpty {

    @BeforeTemplate
    boolean lengthZero(String s) {
        return s.length() == 0;
    }

    @BeforeTemplate
    boolean lengthZeroFlip(String s) {
        return 0 == s.length();
    }

    @BeforeTemplate
    boolean lengthSmallerOne(String s) {
        return s.length() < 1;
    }

    @BeforeTemplate
    boolean lengthSmallerOneFlipped(String s) {
        return 1 > s.length();
    }

    @BeforeTemplate
    boolean lengthEqualEmpty(String s) {
        return s.equals("");
    }

    @BeforeTemplate
    boolean lengthEqualEmptyFlip(String s) {
        return "".equals(s);
    }

    @AfterTemplate
    boolean isEmpty(String s) {
        return s.isEmpty();
    }
}

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

import io.moderne.devcenter.DevCenter;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class DevCenterTest {

    @Test
    void valid() throws Exception {
        Recipe recipe = Environment.builder()
          .scanRuntimeClasspath("com.yourorg", "io.moderne.devcenter", "org.openrewrite.java.security")
          .build()
          .activateRecipes("com.yourorg.DevCenter");

        // Fails on mistakes the Moderne Platform and CLI would reject, such as two cards with the same name
        DevCenter devCenter = new DevCenter(recipe);
        devCenter.validate();

        assertThat(devCenter.getUpgradesAndMigrations())
          .extracting(DevCenter.Card::getName)
          .containsExactly(
            "Move to Spring Boot 4.1",
            "Move to Java 25",
            "Move to Angular 22",
            "Replace Guava Lists.newArrayList");
        assertThat(devCenter.getSecurity()).isNotNull();
    }
}

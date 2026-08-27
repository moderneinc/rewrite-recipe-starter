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
package com.yourorg.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class SensitiveDataInLogsReport extends DataTable<SensitiveDataInLogsReport.Row> {

    public SensitiveDataInLogsReport(Recipe recipe) {
        super(recipe, "Sensitive data in logs",
                "Sensitive values that taint tracking proves reach a logging call.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The path to the source file containing the flow.")
        String sourcePath;

        @Column(displayName = "Source",
                description = "The expression producing the sensitive value.")
        String source;

        @Column(displayName = "Sink",
                description = "The logging argument the sensitive value flows into.")
        String sink;
    }
}

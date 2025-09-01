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

import com.yourorg.table.TodoCommentsReport;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;

@Value
@EqualsAndHashCode(callSuper = false)
public class TrackJavaTodos extends Recipe {

    transient TodoCommentsReport todoCommentsTable = new TodoCommentsReport(this);

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
                Space s = super.visitSpace(space, loc, ctx);
                for (Comment comment : s.getComments()) {
                    // Let's just match TextComments and ignore Javadoc comments
                    if (comment instanceof TextComment) {
                        String text = ((TextComment) comment).getText().trim();
                        if (text.contains("TODO")) {
                            String sourcePath = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString();
                            todoCommentsTable.insertRow(ctx, new TodoCommentsReport.Row(
                                    sourcePath,
                                    text,
                                    getCursor().getValue().getClass().toString()));
                        }
                    }
                }
                return s;
            }
        };
    }
}

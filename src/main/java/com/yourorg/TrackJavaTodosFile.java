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
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class TrackJavaTodosFile extends ScanningRecipe<TrackJavaTodosFile.TodoComments> {

    @Override
    public String getDisplayName() {
        return "Track TODOs from Java comments";
    }

    @Override
    public String getDescription() {
        return "Scans Java source comments for TODOs and collects them in a file.";
    }

    @Option(displayName = "Header",
            description = "Header for TODO.md. Defaults to `## To Do List` if not provided.",
            example = "## To Do List",
            required = false)
    @Nullable
    String header;

    public static class TodoComments {
        boolean foundTodoFile;
        Set<String> todos = new LinkedHashSet<>();
    }

    @Override
    public TodoComments getInitialValue(ExecutionContext ctx) {
        return new TodoComments();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(TodoComments acc) {
        JavaIsoVisitor<ExecutionContext> extractJavaTodosVisitor = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                for (Comment comment : space.getComments()) {
                    // Let's just match TextComments and ignore Javadoc comments
                    if (comment instanceof TextComment) {
                        String text = ((TextComment) comment).getText();
                        if (text.contains("TODO")) {
                            acc.todos.add(text.trim());
                        }
                    }
                }
                return space;
            }
        };

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                if (sourceFile.getSourcePath().endsWith("TODO.md")) {
                    acc.foundTodoFile = true;
                } else if (extractJavaTodosVisitor.isAcceptable(sourceFile, ctx)) {
                    extractJavaTodosVisitor.visit(tree, ctx);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(TodoComments acc, ExecutionContext ctx) {
        if (acc.foundTodoFile) {
            return emptyList();
        }
        // If the file was not found, create it
        return PlainTextParser.builder().build()
                // We start with an empty string that we then append to in the visitor
                .parse("")
                // Be sure to set the source path for any generated file to specify where to put it when the recipe run is completed
                .map(it -> (SourceFile) it.withSourcePath(Paths.get("TODO.md")))
                .collect(toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(TodoComments acc) {
        return Preconditions.check(
                // Only modify the text if it is the TODO.md file
                new FindSourceFiles("TODO.md"),
                new PlainTextVisitor<ExecutionContext>() {
                    @Override
                    public PlainText visitText(PlainText text, ExecutionContext ctx) {
                        PlainText t = super.visitText(text, ctx);
                        // OpenRewrite uses referential equality checks to detect when the LST returned by a method is different from the one that was passed into the method.
                        // If a referentially un-equal object with otherwise the same contents is returned it can result in empty changes.
                        // Thanks to String interning all Strings with equivalent content are the same instance and therefore referentially equal.
                        String prefix = Optional.ofNullable(header).orElse("## To Do List");
                        String content = String.join("\n", acc.todos);
                        return t.withText(prefix + "\n" + content);
                    }
                }
        );
    }
}

package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class TrackJavaTodos extends ScanningRecipe<TrackJavaTodos.TodoComments> {

    @Override
    public String getDisplayName() {
        return "Track TODOs from Java comments";
    }

    @Override
    public String getDescription() {
        return "Scans Java source comments for TODOs and collects them.";
    }

    @Option(displayName = "Header",
            description = "Header for TODO.md.",
            example = "## To Do List",
            required = false)
    @Nullable
    String header;

    public static class TodoComments {
        boolean foundTodoFile;
        LinkedHashSet<String> todos = new LinkedHashSet<>();
    }

    @Override
    public TodoComments getInitialValue(ExecutionContext ctx) {
        return new TodoComments();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(TodoComments acc) {
        JavaIsoVisitor<ExecutionContext> javaIsoVisitor = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                for (Comment comment : space.getComments()) {
                    // Let's just match TextComments and ignore Javadoc comments
                    if (comment instanceof TextComment) {
                        String c = ((TextComment) comment).getText();
                        if (c.contains("TODO")) {
                            acc.todos.add(c);
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
                SourceFile s = (SourceFile) tree;
                if ("TODO.md".equals(s.getSourcePath().toString())) {
                    acc.foundTodoFile = true;
                }
                if (javaIsoVisitor.isAcceptable(s, ctx)) {
                    javaIsoVisitor.visit(tree, ctx);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(TodoComments acc, ExecutionContext ctx) {
        if (acc.foundTodoFile) {
            return Collections.emptyList();
        }
        // If the file was not found, create it
        return PlainTextParser.builder().build()
                // We start with an empty string that we then append to in the visitor
                .parse("")
                // Be sure to set the source path for any generated file, so that the visitor can find it
                .map(it -> (SourceFile) it.withSourcePath(Paths.get("TODO.md")))
                .collect(Collectors.toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(TodoComments acc) {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                PlainText t = super.visitText(text, ctx);
                // If the file is not TODO.md, don't modify it
                if (!"TODO.md".equals(t.getSourcePath().toString())) {
                    return t;
                }
                return t.withText(
                    acc.todos.stream()
                        .map(String::trim)
                        .collect(Collectors.joining("\n", (header == null ? "## To Do List" : header) + "\n", "\n"))
                );
            }
        };
    }
}

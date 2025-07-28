package com.yourorg;

import com.yourorg.table.TodoCommentsReport;
import com.yourorg.trait.TodoComment;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class TrackTodos extends ScanningRecipe<TrackTodos.TodoComments> {

    transient TodoCommentsReport todoCommentsTable = new TodoCommentsReport(this);

    @Override
    public String getDisplayName() {
        return "Track TODOs from Java, YAML, or XML comments";
    }

    @Override
    public String getDescription() {
        return "Scans Java, YAML, and XML source comments for TODOs and collects them.";
    }

    @Option(displayName = "Header",
            description = "Header for TODO.md. Defaults to `## To Do List` if not provided.",
            example = "## To Do List",
            required = false)
    @Nullable
    String header;

    public static class TodoComments {
        boolean foundTodoFile;
        LinkedHashSet<TodoComment> todos = new LinkedHashSet<>();
    }

    @Override
    public TodoComments getInitialValue(ExecutionContext ctx) {
        return new TodoComments();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(TodoComments acc) {
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
                return new TodoComment.Matcher()
                        .asVisitor((todo, context) -> {
                            acc.todos.add(todo);
                            return todo.getTree();
                        })
                        .visit(tree, ctx);
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(TodoComments acc, ExecutionContext ctx) {
        // Insert data table rows first
        for (TodoComment todo : acc.todos) {
            for (String item : todo.getTodos()) {
                String sourcePath = todo.getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString();
                //System.out.println(sourcePath);
                //System.out.println(item);
                //System.out.println(todo.getTree().getClass().toString());
                //System.out.println();
                todoCommentsTable.insertRow(ctx, new TodoCommentsReport.Row(sourcePath, item, todo.getTree().getClass().toString()));
            }
        }
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
                // OpenRewrite uses referential equality checks to detect when the LST returned by a method is different than the one that was passed into the method.
                // If a referentially un-equal object with otherwise the same contents is returned it can result in empty changes.
                // Thanks to String interning all Strings with equivalent content are the same instance and therefore referentially equal.
                return t.withText(
                        acc.todos.stream()
                                .flatMap(todo -> todo.getTodos().stream())
                                .map(String::trim)
                                .collect(Collectors.joining("\n", (header == null ? "## To Do List" : header) + "\n", "\n"))
                );
            }
        };
    }
}

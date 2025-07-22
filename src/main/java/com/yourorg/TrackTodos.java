package com.yourorg;

import com.yourorg.table.TodoCommentsReport;
import com.yourorg.trait.TodoComment;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Paths;
import java.util.*;
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
            description = "Header for TODO.md.",
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
                StringBuilder sb = new StringBuilder();
                for (TodoComment todo : acc.todos) {
                    for (String item : todo.getTodos()) {
                        sb.append(item).append("\n");
                        String sourcePath = todo.getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString();
                        System.out.println(sourcePath);
                        System.out.println(item);
                        System.out.println(todo.getTree().getClass().toString());
                        System.out.println();
                        todoCommentsTable.insertRow(ctx, new TodoCommentsReport.Row(sourcePath, item, todo.getTree().getClass().toString()));
                    }
                }

                String allComments = sb.toString();
                String headerText = header == null ? "## To Do List" : header;
                // Append the comments to the end of the file
                return t.withText((headerText + "\n" + allComments).trim());
            }
        };
    }
}

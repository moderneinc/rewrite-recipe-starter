package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptyList;

// TODO - This is a placeholder for a scanning recipe.
// Implement a recipe that finds any comments in Java source files that contain `TODO`, and add them to a file called `TODO.md`.
// You're done when all of the tests in `TrackJavaTodosFileTest` pass.
// Implement a scanner that collects `TODO` comments, a generate that creates the `TODO.md` file if needed, and a visitor that inserts the collected TODOs into the file.
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
        // TODO: implement a scanner that looks for TODO comments in Java source files
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(TodoComments acc, ExecutionContext ctx) {
        // TODO: implement a method that generates TODO.md if there are any todos found and the file hasn't been created yet
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(TodoComments acc) {
        // TODO: implement a method to insert all todos to the TODO.md file
        return TreeVisitor.noop();
    }
}

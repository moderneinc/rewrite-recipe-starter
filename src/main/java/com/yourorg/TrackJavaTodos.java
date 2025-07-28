package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;

import java.util.LinkedHashSet;

// TODO - This is a placeholder for a scanning recipe.
// Implement a recipe that finds any comments in Java source files that contain `TODO`, and add them to a file called `TODO.md`.
// You're done when all of the tests in `TrackJavaTodosTest` pass.
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
            description = "Header for TODO.md. Defaults to `## To Do List` if not provided.",
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
        return new TreeVisitor<Tree, ExecutionContext>() {

        };
    }
}

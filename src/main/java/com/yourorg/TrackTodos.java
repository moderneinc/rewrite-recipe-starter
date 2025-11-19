package com.yourorg;

import com.yourorg.table.TodoCommentsReport;
import com.yourorg.trait.TodoComment;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptyList;

// TODO - This is a placeholder for a scanning recipe that uses traits and data tables.
// Implement a recipe that finds any comments in Java, XML, or YAML source files that contain `TODO`, and add them to a file called `TODO.md`.
// Also store the data in a data table using TodoCommentsReport.
// You're done when all of the tests in `TrackTodosTest` pass.
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
        Set<TodoComment> todos = new LinkedHashSet<>();
    }

    @Override
    public TodoComments getInitialValue(ExecutionContext ctx) {
        return new TodoComments();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(TodoComments acc) {
        // TODO Leverage TodoComment.Matcher to find TODO comments in Java, YAML, and XML files.
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(TodoComments acc, ExecutionContext ctx) {
        // TODO Insert a row for each todo comment into todoCommentsTable
        // TODO Potentially create a new plain text source file named TODO.md
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(TodoComments acc) {
        // TODO Write all the todo comments to TODO.md
        return TreeVisitor.noop();
    }
}

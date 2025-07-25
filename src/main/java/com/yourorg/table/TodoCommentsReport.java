package com.yourorg.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class TodoCommentsReport extends DataTable<TodoCommentsReport.Row> {

    public TodoCommentsReport(Recipe recipe) {
        super(recipe,
                "Todo comments report",
                "Records Todo comments and the type of element they are attached to.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The path to the source file containing the comment.")
        String sourcePath;

        @Column(displayName = "Comment text",
                description = "The text of the comment.")
        String commentText;

        @Column(displayName = "Element type",
                description = "The class and element type that the comment is attached to.")
        String elementType;
    }
}

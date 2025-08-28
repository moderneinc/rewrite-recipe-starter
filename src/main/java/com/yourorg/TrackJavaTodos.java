package com.yourorg;

import com.yourorg.table.TodoCommentsReport;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
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
        return "Track TODOs from Java comments";
    }

    @Override
    public String getDescription() {
        return "Scans Java source comments for TODOs and collects them.";
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

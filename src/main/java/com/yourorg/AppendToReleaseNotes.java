package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = true)
public class AppendToReleaseNotes extends ScanningRecipe<AppendToReleaseNotes.Accumulator> {


    @Override
    public String getDisplayName() {
        return "Append to release notes";
    }

    @Override
    public String getDescription() {
        return "Adds the specified line to RELEASE.md.";
    }

    @Option(displayName = "Message",
            description = "Message to append to the bottom of RELEASE.md.")
    String message;


    public static class Accumulator {
        boolean found;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                if(tree instanceof SourceFile) {
                    Path sourcePath = ((SourceFile) tree).getSourcePath();
                    acc.found |= "RELEASE.md".equals(sourcePath.toString());
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        if(acc.found) {
            return Collections.emptyList();
        }
        return PlainTextParser.builder().build()
                .parse("")
                .map(it -> (SourceFile) it.withSourcePath(Paths.get("RELEASE.md")))
                .collect(Collectors.toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                PlainText t = super.visitText(text, executionContext);
                if(!"RELEASE.md".equals(t.getSourcePath().toString())) {
                    return t;
                }
                if(t.getText().contains(message)) {
                    return t;
                }
                return t.withText(t.getText() + "\n" + message);
            }
        };
    }
}

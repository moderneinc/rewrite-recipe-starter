/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
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
            description = "Message to append to the bottom of RELEASE.md.",
            example = "## 1.0.0\n\n- New feature")
    String message;

    // The shared state between the scanner and the visitor. The custom class ensures we can easily extend the recipe.
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
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    Path sourcePath = ((SourceFile) tree).getSourcePath();
                    acc.found |= "RELEASE.md".equals(sourcePath.toString());
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        if (acc.found) {
            return Collections.emptyList();
        }
        // If the file was not found, create it
        return PlainTextParser.builder().build()
                // We start with an empty string that we then append to in the visitor
                .parse("")
                // Be sure to set the source path for any generated file, so that the visitor can find it
                .map(it -> (SourceFile) it.withSourcePath(Paths.get("RELEASE.md")))
                .collect(Collectors.toList());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                PlainText t = super.visitText(text, ctx);
                // If the file is not RELEASE.md, don't modify it
                if (!"RELEASE.md".equals(t.getSourcePath().toString())) {
                    return t;
                }
                // If the file already contains the message, don't append it again
                if (t.getText().contains(message)) {
                    return t;
                }
                // Append the message to the end of the file
                return t.withText(t.getText() + "\n" + message);
            }
        };
    }
}

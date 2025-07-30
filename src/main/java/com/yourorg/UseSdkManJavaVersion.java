package com.yourorg;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Paths;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class UseSdkManJavaVersion extends ScanningRecipe<UseSdkManJavaVersion.Accumulator> {

    @Override
    public String getDisplayName() {
        return "Use SDKMan java version";
    }

    @Override
    public String getDescription() {
        //language=Markdown
        return "Examines a multi-project build, determines the Java version in use via available markers, and sets up or updates a .sdkmanrc file in the project root.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    public static class Accumulator {
        boolean sdkmanrcExists = false;
        int javaVersion = -1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof PlainText) {
                    // Check if the current tree is a PlainText file, which could be a .sdkmanrc file
                    acc.sdkmanrcExists |= ((PlainText) tree).getSourcePath().endsWith(".sdkmanrc");
                } else if (tree instanceof JavaSourceFile) {
                    // we have a java file which usually has the project, source set and java version markers
                    // Visit the compilation unit to find Java version markers
                    tree.getMarkers()
                            .findFirst(JavaVersion.class)
                            .ifPresent(version -> acc.javaVersion = Math.max(acc.javaVersion, version.getMajorVersion()));
                }
                return super.visit(tree, ctx);
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        if (!acc.sdkmanrcExists && 8 <= acc.javaVersion) {
            // Create an empty .sdkmanrc file for now, and add content in getVisitor(Accumulator acc)
            return singletonList(
                    PlainText.builder()
                            .text("")
                            .sourcePath(Paths.get(".sdkmanrc"))
                            .build()
            );
        }
        // If a .sdkmanrc file already exists, we will not generate a new one
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(
                new FindSourceFiles(".sdkmanrc"),
                new PlainTextVisitor<ExecutionContext>() {
                    @Override
                    public PlainText visitText(PlainText text, ExecutionContext ctx) {
                        // No change needed if no java version is set or already matches
                        if (acc.javaVersion < 8 || text.getText().contains("java=" + acc.javaVersion)) {
                            return text;
                        }
                        // If the file already contains the highest java version, we do nothing
                        if (text.getText().contains("java=")) {
                            // If the file contains a java version but not the highest one, we will update it
                            return text.withText(text.getText().replaceAll("java=\\d+", "java=" + acc.javaVersion));
                        }
                        // If the file does not contain a java version, we will add it
                        return text.withText(text.getText() + "\njava=" + acc.javaVersion);
                    }
                }
        );
    }
}

package com.yourorg;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Paths;
import java.util.*;

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
        final Map<SourceSetCoordinate, Integer> javaVersions = new HashMap<>();

        @Value
        static class SourceSetCoordinate {
            JavaProject javaProject;
            String sourceSetName;
        }
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext context, Cursor parent) {
                if (tree instanceof J.CompilationUnit cu) {
                    // we have a java file which usually has the project, source set and java version markers
                    // Visit the compilation unit to find Java version markers
                    Optional<JavaVersion> javaVersion = cu.getMarkers().findFirst(JavaVersion.class);
                    Optional<JavaProject> javaProject = cu.getMarkers().findFirst(JavaProject.class);

                    if (javaVersion.isPresent() && javaProject.isPresent()) {
                        // is we have all the information we can store it in the accumulator
                        String sourceSetName = cu.getMarkers().findFirst(JavaSourceSet.class).map(JavaSourceSet::getName).orElse("main");
                        acc.javaVersions.putIfAbsent(new Accumulator.SourceSetCoordinate(javaProject.get(), sourceSetName), javaVersion.get().getMajorVersion());
                    }
                } else if (tree instanceof PlainText) {
                    // Check if the current tree is a PlainText file, which could be a .sdkmanrc file
                    acc.sdkmanrcExists |= ((PlainText) tree).getSourcePath().endsWith(".sdkmanrc");
                }
                return super.visit(tree, context, parent);
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        if (acc.sdkmanrcExists) {
            // If a .sdkmanrc file already exists, we will not generate a new one
            return Collections.emptyList();
        }
        // we nhave to create a .sdkmanrc file to later ad the java version configuration
        return Collections.singletonList(
                PlainText.builder()
                        .text("")
                        .sourcePath(Paths.get(".sdkmanrc"))
                        .build()
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(new FindSourceFiles(".sdkmanrc"),
                new PlainTextVisitor<>() {
                    @Override
                    public PlainText visitText(PlainText text, ExecutionContext ctx) {
                        int highestJavaVersion = acc.javaVersions.values().stream().max(Integer::compareTo).orElse(8);

                        if (text.getText().contains("java=" + highestJavaVersion)) {
                            // If the file already contains the highest java version, we do nothing
                            return text;
                        } else if (text.getText().contains("java=")) {
                            // If the file contains a java version but not the highest one, we will update it
                            return text.withText(text.getText().replaceAll("java=\\d+", "java=" + highestJavaVersion));
                        } else {
                            // If the file does not contain a java version, we will add it
                            return text.withText(text.getText() + "\njava=" + highestJavaVersion);
                        }
                    }
                }
        );
    }
}
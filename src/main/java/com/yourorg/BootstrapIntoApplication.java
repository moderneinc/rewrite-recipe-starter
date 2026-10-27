package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.yaml.MergeYaml;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * This recipe is similar in structure to AppendToReleaseNotes, but uses more advanced techniques.
 * This recipe demonstrates:
 * - Copying information from one file into another
 * - Using markers like JavaSourceSet to operate correctly in single-module and multi-module projects
 * - Organizing logic into multiple visitors to keep separate concerns separate
 * - Using other recipes, in this case MergeYaml, as building blocks
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class BootstrapIntoApplication extends ScanningRecipe<BootstrapIntoApplication.Accumulator> {
    String displayName = "Merge Spring bootstrap.yml into application.yml";

    String description = "Merge all contents of any bootstrap.yml file into its corresponding application.yml. " +
               "Will create an application.yml if none already exists. " +
               "If an applicaiton.yml already exists and its values conflict with those in bootstrap.yml the values in " +
               "bootstrap.yml are given priority.";

    @Option(displayName = "Delete bootstrap",
            description = "When set to true delete the bootstrap.yml after migrating all its properties to application.yml. Default false.",
            example = "true",
            required = false)
    boolean deleteBootstrap;

    /**
     * A common recipe authoring mistake is to treat all repositories like single module repositories.
     * But there might be multiple modules with their own application/bootstrap, and a single project might have
     * separate application/bootstrap yaml in its main and test source sets.
     * This accumulator keeps track of all of this using the OpenRewrite-provided JavaSourceSet marker.
     */
    public static class Accumulator {

        /**
         * Keep track of the bootstrap.yml in each source set where they appear, keyed by the JavaSourceSet id.
         * The accumulator stays in memory for the whole recipe run, so it keeps the path and printed contents only:
         * a JavaSourceSet holds a whole classpath and a Yaml.Documents a whole LST.
         */
        Map<UUID, BootstrapYaml> sourceSetToBootstrapYaml = new HashMap<>();
        /**
         * Keep track of which source sets already have an application.yml
         */
        Set<UUID> sourceSetsWithExistingApplicationYaml = new HashSet<>();
        /**
         * An application.yml generated in an earlier cycle has no JavaSourceSet marker, so is only recognized by path
         */
        Set<Path> applicationYamlPaths = new HashSet<>();
    }

    @Value
    static class BootstrapYaml {
        Path sourcePath;
        String contents;
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
                // When there are multiple concerns to juggle it can often be easier to maintain several
                // individual, specific visitors rather than one sprawling, general visitor
                new BootstrapScanner(acc).visit(tree, ctx);
                new ApplicationScanner(acc).visit(tree, ctx);

                // Note the lack of calls to super.visit*() methods throughout this recipe
                // It saves CPU+time to elide this traversal when it is not necessary
                return tree;
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class BootstrapScanner extends YamlIsoVisitor<ExecutionContext> {
        Accumulator acc;

        @Override
        public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
            if (!documents.getSourcePath().toString().endsWith("bootstrap.yml")) {
                return documents;
            }
            documents.getMarkers().findFirst(JavaSourceSet.class).ifPresent(jss -> acc.sourceSetToBootstrapYaml
                    .put(jss.getId(), new BootstrapYaml(documents.getSourcePath(), documents.printAll())));
            return documents;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ApplicationScanner extends YamlIsoVisitor<ExecutionContext> {
        Accumulator acc;

        @Override
        public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
            if (!documents.getSourcePath().toString().endsWith("application.yml")) {
                return documents;
            }
            acc.applicationYamlPaths.add(documents.getSourcePath());
            documents.getMarkers().findFirst(JavaSourceSet.class).ifPresent(jss ->
                    acc.sourceSetsWithExistingApplicationYaml.add(jss.getId()));
            return documents;
        }
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        if (acc.sourceSetToBootstrapYaml.isEmpty()) {
            return emptyList();
        }
        List<SourceFile> results = new ArrayList<>();
        YamlParser yp = YamlParser.builder().build();
        for (Map.Entry<UUID, BootstrapYaml> sourceSetBootstrapEntry : acc.sourceSetToBootstrapYaml.entrySet()) {
            BootstrapYaml bootstrap = sourceSetBootstrapEntry.getValue();
            Path applicationYamlPath = bootstrap.getSourcePath().resolveSibling("application.yml");
            if (!acc.sourceSetsWithExistingApplicationYaml.contains(sourceSetBootstrapEntry.getKey()) &&
                !acc.applicationYamlPaths.contains(applicationYamlPath)) {
                results.addAll(yp.parse(bootstrap.getContents())
                        .map(it -> (SourceFile) it.withSourcePath(applicationYamlPath))
                        .collect(toList()));
            }
        }

        return results;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t;
                t = new BootstrapVisitor(deleteBootstrap, acc).visit(tree, ctx);
                return new ApplicationVisitor(acc).visit(t, ctx);
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class BootstrapVisitor extends YamlIsoVisitor<ExecutionContext> {
        boolean deleteBootstrap;
        Accumulator acc;

        @Override
        public Yaml.@Nullable Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
            if (documents.getSourcePath().toString().endsWith("bootstrap.yml") && deleteBootstrap) {
                // Returning "null" is how you tell OpenRewrite to delete an individual LST element or an entire file.
                //noinspection DataFlowIssue
                return null;
            }
            return documents;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ApplicationVisitor extends YamlIsoVisitor<ExecutionContext> {
        Accumulator acc;

        @Override
        public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
            if (!documents.getSourcePath().toString().endsWith("application.yml")) {
                return documents;
            }
            Optional<JavaSourceSet> maybeJss = documents.getMarkers().findFirst(JavaSourceSet.class);
            if (!maybeJss.isPresent()) {
                return documents;
            }
            BootstrapYaml bootstrapYaml = acc.sourceSetToBootstrapYaml.get(maybeJss.get().getId());
            if (bootstrapYaml == null) {
                return documents;
            }
            return (Yaml.Documents) new MergeYaml("$", bootstrapYaml.getContents(), false, null, null, null, null, true)
                    .getVisitor()
                    .visitNonNull(documents, ctx);
        }
    }
}

package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
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
 * - Pairing files by path to operate correctly in single-module and multi-module projects
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
     * This accumulator keeps track of all of this by the path of the application.yml next to each bootstrap.yml.
     * It holds paths and text rather than LSTs or markers like JavaSourceSet, as it stays in memory for the whole
     * scan of what may be a very large repository.
     */
    public static class Accumulator {
        Map<Path, String> applicationYamlToBootstrapContents = new HashMap<>();
        Set<Path> existingApplicationYaml = new HashSet<>();
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
            acc.applicationYamlToBootstrapContents.put(applicationYamlNextTo(documents), documents.printAll());
            return documents;
        }
    }

    private static Path applicationYamlNextTo(Yaml.Documents bootstrapYaml) {
        return bootstrapYaml.getSourcePath().resolveSibling("application.yml");
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
            acc.existingApplicationYaml.add(documents.getSourcePath());
            return documents;
        }
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        if (acc.applicationYamlToBootstrapContents.isEmpty()) {
            return emptyList();
        }
        List<SourceFile> results = new ArrayList<>();
        YamlParser yp = YamlParser.builder().build();
        for (Path applicationYaml : acc.applicationYamlToBootstrapContents.keySet()) {
            if (!acc.existingApplicationYaml.contains(applicationYaml)) {
                results.addAll(yp.parse("")
                        .map(it -> (SourceFile) it.withSourcePath(applicationYaml))
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
            String bootstrapContents = acc.applicationYamlToBootstrapContents.get(documents.getSourcePath());
            if (bootstrapContents == null) {
                return documents;
            }
            return (Yaml.Documents) new MergeYaml("$", bootstrapContents, false, null, null, null, null, true)
                    .getVisitor()
                    .visitNonNull(documents, ctx);
        }
    }
}

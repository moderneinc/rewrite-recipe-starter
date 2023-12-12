package com.yourorg;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.yaml.ChangePropertyValue;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpdateConcoursePipeline extends Recipe {
    @Override
    public String getDisplayName() {
        return "Update concourse pipeline";
    }

    @Override
    public String getDescription() {
        return "Update the tag filter on concourse pipelines.";
    }

    @Option(displayName = "New tag filter version",
            description = "tag filter version.")
    String version;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new FindSourceFiles("ci/pipeline*.yml").getVisitor(),
                        new FindSourceFiles("ci/pipeline*.yaml").getVisitor()),
                new YamlIsoVisitor<ExecutionContext>() {

                    @Override
                    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                        if (e.getKey().getValue().equals("source")) {
                            Yaml.Block value = e.getValue();
                            if(!(value instanceof Yaml.Mapping)) {
                                return e;
                            }
                            Yaml.Mapping mapping = (Yaml.Mapping) value;
                            Yaml.Mapping.Entry uriEntry = null;
                            Yaml.Mapping.Entry tagFilter = null;
                            for (Yaml.Mapping.Entry mappingEntry : mapping.getEntries()) {
                                if("uri".equals(mappingEntry.getKey().getValue())) {
                                    uriEntry = mappingEntry;
                                } else if("tag_filter".equals(mappingEntry.getKey().getValue())) {
                                    tagFilter = mappingEntry;
                                }
                            }
                            if(uriEntry == null || tagFilter == null) {
                                return e;
                            }
                            if(!(uriEntry.getValue() instanceof Yaml.Scalar) || !(tagFilter.getValue() instanceof Yaml.Scalar)) {
                                return e;
                            }
                            Yaml.Scalar uriValue = (Yaml.Scalar) uriEntry.getValue();
                            if(!uriValue.getValue().contains(".git")) {
                                return e;
                            }
                            Yaml.Scalar tagFilterValue = (Yaml.Scalar) tagFilter.getValue();
                            if(version.equals(tagFilterValue.getValue())) {
                                return e;
                            }
                            return (Yaml.Mapping.Entry) new ChangePropertyValue("source.tag_filter", version, null, null, null)
                                    .getVisitor()
                                    .visitNonNull(e, ctx);
                        }
                        return e;
                    }
                }
        );
    }
}

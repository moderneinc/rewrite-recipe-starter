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
import org.openrewrite.yaml.ChangePropertyValue;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = false)
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
            description = "tag filter version.",
            example = "8.2.0")
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
                        if ("source".equals(e.getKey().getValue())) {
                            Yaml.Block value = e.getValue();
                            if (!(value instanceof Yaml.Mapping)) {
                                return e;
                            }
                            Yaml.Mapping mapping = (Yaml.Mapping) value;
                            Yaml.Mapping.Entry uriEntry = null;
                            Yaml.Mapping.Entry tagFilter = null;
                            for (Yaml.Mapping.Entry mappingEntry : mapping.getEntries()) {
                                if ("uri".equals(mappingEntry.getKey().getValue())) {
                                    uriEntry = mappingEntry;
                                } else if ("tag_filter".equals(mappingEntry.getKey().getValue())) {
                                    tagFilter = mappingEntry;
                                }
                            }
                            if (uriEntry == null || tagFilter == null) {
                                return e;
                            }
                            if (!(uriEntry.getValue() instanceof Yaml.Scalar) || !(tagFilter.getValue() instanceof Yaml.Scalar)) {
                                return e;
                            }
                            Yaml.Scalar uriValue = (Yaml.Scalar) uriEntry.getValue();
                            if (!uriValue.getValue().contains(".git")) {
                                return e;
                            }
                            Yaml.Scalar tagFilterValue = (Yaml.Scalar) tagFilter.getValue();
                            if (version.equals(tagFilterValue.getValue())) {
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

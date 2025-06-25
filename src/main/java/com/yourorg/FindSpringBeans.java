/*
 * Copyright 2025 the original author or authors.
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

import com.yourorg.table.SpringBeansReport;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.trait.Annotated;
import org.openrewrite.java.trait.Literal;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindSpringBeans extends Recipe {

    transient SpringBeansReport beansTable = new SpringBeansReport(this);

    @Override
    public String getDisplayName() {
        return "Find Spring beans";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Find all Spring bean names used in your application.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // We'll use a trait here. A trait is an arbitrary concept in your code that has a name, like "a global variable" or "a spring bean".
        // Use the existing `org.openrewrite.java.trait.Annotated` trait to easily match annotations, and annotated elements
        // In this case we need the values of the `@Bean` annotations
        return new Annotated.Matcher("@org.springframework.context.annotation.Bean")
                // Convert the trait into a visitor to get access to the annotations, and use their attributes
                .asVisitor((annotated, ctx) -> {
                    // Get name value from the annotation or the default string value if no named annotation arguments are mentioned
                    String beanName = annotated.getDefaultAttribute("name")
                            .map(Literal::getString)
                            // If no value is present in the annotation, we fall back to the method name
                            .orElseGet(() -> annotated.getCursor().getParentTreeCursor().<J.MethodDeclaration>getValue().getSimpleName());

                    // Insert the bean name into the SpringBeans report
                    String sourcePath = annotated.getCursor().firstEnclosingOrThrow(JavaSourceFile.class).getSourcePath().toString();
                    beansTable.insertRow(ctx, new SpringBeansReport.Row(sourcePath, beanName));

                    // Return a modified LST element with an added search result marker calling out the bean name
                    return SearchResult.found(annotated.getTree(), beanName);
                });
    }
}

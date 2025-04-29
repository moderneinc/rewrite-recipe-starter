/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yourorg;

import com.yourorg.table.SpringBeans;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.trait.Literal;
import org.openrewrite.java.trait.Traits;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindSpringBeans extends Recipe {

    transient SpringBeans beansTable = new SpringBeans(this);

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
        // Use the `org.openrewrite.java.trait.Annotated` trait to easily match annotations, and annotated elements
        return Traits.annotated("@org.springframework.context.annotation.Bean")
                // Convert the trait into a visitor to get access to the annotations, and use their attributes
                .asVisitor((annotated, ctx) -> {
                    String beanName = annotated.getDefaultAttribute("name")
                            .map(Literal::getString)
                            // If no name is present in the annotation, we fall back to the method name
                            .orElseGet(() -> annotated.getCursor().getParentTreeCursor().<J.MethodDeclaration>getValue().getSimpleName());

                    // Insert the bean name into the SpringBeans report
                    String sourcePath = annotated.getCursor().firstEnclosingOrThrow(JavaSourceFile.class).getSourcePath().toString();
                    beansTable.insertRow(ctx, new SpringBeans.Row(sourcePath, beanName));

                    // Return a modified LST element with an added search result marker calling out the bean name
                    return SearchResult.found(annotated.getTree(), beanName);
                });
    }
}

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
import com.yourorg.trait.SpringBean;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindSpringBeans extends Recipe {

    transient SpringBeans beans = new SpringBeans(this);

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

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                SpringBean.Matcher matcher = new SpringBean.Matcher();
                for (J.Annotation annotation : service(AnnotationService.class).getAllAnnotations(getCursor())) {
                    // Create an Optional<SpringBean> custom Trait from the annotation passing in the method declaration
                    //     as the bean name can come from the method name if no value is present in the annotation
                    //          \
                    //           \                   If the matcher is found, we will map it to a SearchResult marked one
                    //            \                            /
                    m = matcher.get(annotation, getCursor()).map(springBean -> {
                        // Calculate the name of the bean in the `springBean` Trait, hiding the complexity of the LST elements
                        String name = springBean.getName();

                        // Insert the bean name into the SpringBeans report
                        beans.insertRow(ctx, new SpringBeans.Row(
                                getCursor().firstEnclosingOrThrow(JavaSourceFile.class).getSourcePath().toString(),
                                name
                        ));

                        // Mark the method declaration with the bean name found marker
                        return SearchResult.found(method, name);
                    }).orElse(m);
                    //         \
                    // If the matcher is not found, we will return the method declaration as is.
                }
                return m;
            }
        };
    }
}
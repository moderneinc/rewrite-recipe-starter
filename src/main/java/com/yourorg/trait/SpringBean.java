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
package com.yourorg.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.trait.Annotated;
import org.openrewrite.java.trait.Literal;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.Optional;

import static org.openrewrite.java.trait.Traits.annotated;

@Value
public class SpringBean implements Trait<Tree> {
    Cursor cursor;

    // As the Matcher's test method always creates this Trait with MethodDeclaration as the tree type,
    // we can try to get the name of the bean from the annotation.
    // If no name is present, we fall back to the method name.
    // If no annotation is present, this Trait will have never been created as the test method returned null
    public @Nullable String getName() {
        if (getTree() instanceof J.MethodDeclaration) {
            J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) getTree();
            // We're passing the annotation value reading to another Trait which is present in framework by default already
            // This other Trait will act on the annotation LST elements below the current cursor (= lower) to do the actual reading of the property
            Optional<Annotated> annotated = annotated("org.springframework.context.annotation.Bean").lower(getCursor()).findFirst();
            if (annotated.isPresent()) {
                return annotated.get()
                        .getDefaultAttribute("name")
                        .map(Literal::getString)
                        // If no name is present in the annotation, we fall back to the method name
                        .orElseGet(methodDeclaration::getSimpleName);
            }
        }
        return null;
    }

    public static class Matcher extends SimpleTraitMatcher<SpringBean> {
        @Override
        protected @Nullable SpringBean test(Cursor cursor) {
            Object value = cursor.getValue();
            // The @Bean annotation must be present on the method declaration in order to be a Spring bean
            AnnotationMatcher beanAnnotation = new AnnotationMatcher("@org.springframework.context.annotation.Bean");
            // We match on either the annotation
            if (value instanceof J.Annotation) {
                if (beanAnnotation.matches((J.Annotation) value)) {
                    return getSpringBean(cursor.getParentTreeCursor());
                }
                // or the method declaration itself
            } else if (value instanceof J.MethodDeclaration) {
                J.MethodDeclaration method = (J.MethodDeclaration) value;
                for (J.Annotation annotation : method.getLeadingAnnotations()) {
                    if (beanAnnotation.matches(annotation)) {
                        return getSpringBean(cursor);
                    }
                }
            }
            return null;
        }

        // We create the SpringBean trait from the MethodDeclaration itself (hence calling getParentTreeCursor for the annotation use case)
        // so we can fall back to the method name if no annotation value is present
        // If our Trait would only need access to the annotation, we could use the annotation cursor
        // making the Trait above easier to implement
        private SpringBean getSpringBean(Cursor cursor) {
            return new SpringBean(cursor);
        }
    }
}
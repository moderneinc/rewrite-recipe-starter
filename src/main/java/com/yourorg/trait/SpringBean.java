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

    private static final String BEAN_ANNOTATION = "org.springframework.context.annotation.Bean";
    private static final AnnotationMatcher BEAN_ANNOTATED = new AnnotationMatcher("@" + BEAN_ANNOTATION);
    // The @Bean annotation must be present on the method declaration in order to be a Spring bean
    private static final Annotated.Matcher beanAnnotationMatcher = annotated(BEAN_ANNOTATED);

    Cursor cursor;

    // We're passing the annotation value reading to ANOTHER TRAIT (=Annotated) which is present in framework by default already
    // During creation of the SpringBean Trait, we already have the Annotated detected, so we can reuse that one.
    Annotated beanAnnotation;

    // As the Matcher's test method always creates this Trait with MethodDeclaration as the tree type,
    // we can try to get the name of the bean from the annotation.
    // If no name is present, we fall back to the method name.
    // If no annotation is present, this Trait will have never been created as the test method returned null
    public @Nullable String getName() {
        if (getTree() instanceof J.MethodDeclaration) {
            return beanAnnotation
                    .getDefaultAttribute("name")
                    .map(Literal::getString)
                    // If no name is present in the annotation, we fall back to the method name
                    .orElseGet(((J.MethodDeclaration) getTree())::getSimpleName);
        }
        return null;
    }

    public static class Matcher extends SimpleTraitMatcher<SpringBean> {
        @Override
        protected @Nullable SpringBean test(Cursor cursor) {
            Object value = cursor.getValue();

            if (value instanceof J.Annotation) {
                Optional<Annotated> annotated = beanAnnotationMatcher.get(cursor);
                if (annotated.isPresent()) {
                    // We create the SpringBean trait from the MethodDeclaration itself (hence calling getParentTreeCursor)
                    // so we can fall back to the method name if no annotation value is present
                    return new SpringBean(cursor.getParentTreeCursor(), annotated.get());
                }
            } else if (value instanceof J.MethodDeclaration) {
                // This Annotated Trait will search for matching elements below the current cursor (= lower) to do the actual reading of the property
                // There can only be one @Bean annotation per method declaration so doing a findFirst is ok
                Optional<Annotated> annotated = beanAnnotationMatcher.lower(cursor).findFirst();
                if (annotated.isPresent()) {
                    return new SpringBean(cursor, annotated.get());
                }
            }

            return null;
        }
    }
}
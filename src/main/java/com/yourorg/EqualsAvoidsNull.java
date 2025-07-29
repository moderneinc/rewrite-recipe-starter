package com.yourorg;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.template.Matcher;
import org.openrewrite.java.template.Matches;
import org.openrewrite.java.template.NotMatches;
import org.openrewrite.java.template.RecipeDescriptor;
import org.openrewrite.java.tree.J;

@RecipeDescriptor(
        name = "EqualsAvoidNull",
        description = "Checks that any combination of String literals is on the left side of an equals() comparison.",
        tags = {"checkstyle", "null-safety"})
// https://checkstyle.sourceforge.io/checks/coding/equalsavoidnull.html
public class EqualsAvoidsNull {

    @BeforeTemplate
    boolean beforeEquals(
            @Matches(LiteralMatcher.class)
            String literal,
            @NotMatches(LiteralMatcher.class)
            String actual) {
        // When `actual` is `null` this throws a NullPointerException!
        return actual.equals(literal);
    }

    @BeforeTemplate
    boolean beforeNotNullEquals(
            String literal,
            @Nullable String actual) {
        // The null check makes this safer, but cumbersome to read and write
        return actual != null && actual.equals(literal);
    }

    @AfterTemplate
    boolean after(String literal, String actual) {
        // Better to put the literal on the left side
        return literal.equals(actual);
    }

    /**
     * Only match literals, not variables or method calls.
     */
    public static class LiteralMatcher implements Matcher<J> {
        @Override
        public boolean matches(J j) {
            return j instanceof J.Literal;
        }
    }
}



package com.yourorg.trait;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;

// TODO - This is a placeholder for a trait.
// Implement a trait that that defines how to match `TODO` comments across Java, YAML, and XML files.
@Value
public class TodoComment implements Trait<Tree> {

    @Getter
    Cursor cursor;

    @Getter
    List<String> todos;

    public static class Matcher extends SimpleTraitMatcher<TodoComment> {
        @Override
        protected @Nullable TodoComment test(Cursor cursor) {
            Object v = cursor.getValue();
            if (!(v instanceof J) && !(v instanceof Yaml) && !(v instanceof Xml)) {
                return null;
            }
            List<String> todos = new ArrayList<>();
            if (v instanceof J) {

            }
            else if (v instanceof Yaml) {

            }
            else if (v instanceof Xml) {

            }
            if (todos.isEmpty()) {
                return null;
            }
            return new TodoComment(cursor, todos);
        }
    }
}

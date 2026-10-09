package com.yourorg.trait;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.tree.Misc;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;

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
                J j = (J) v;
                Space s = j.getPrefix();
                if (s.getComments().isEmpty()) {
                    return null;
                }

                for (Comment comment : s.getComments()) {
                    if (comment instanceof TextComment) {
                        String text = ((TextComment) comment).getText().trim();
                        if (text.contains("TODO")) {
                            todos.add(text);
                        }
                    /* We can optionally handle Javadoc comments

                    } else if (comment instanceof Javadoc.DocComment) {
                        Javadoc.DocComment javadoc = ((Javadoc.DocComment) comment);
                        for (Javadoc javadoc1 : javadoc.getBody()) {
                            if (javadoc1 instanceof Javadoc.Text) {
                                String text = ((Javadoc.Text) javadoc1).getText().trim();
                                if (text.contains("TODO")) {
                                    todos.add(text);
                                }
                            }
                        }

                     */
                    }
                }
            } else if (v instanceof Yaml) {
                String s = ((Yaml) v).getPrefix();
                for (String textLine : s.split("\n")) {
                    if (textLine.contains("#") && textLine.contains("TODO")) {
                        todos.add(textLine.substring(textLine.indexOf('#') + 1).trim());
                    }
                }
            } else if (v instanceof Xml) {
                Xml x = (Xml) v;
                if (x instanceof Xml.Prolog) {
                    for (Misc misc : ((Xml.Prolog) v).getMisc()) {
                        if (misc instanceof Xml.Comment) {
                            String textComment = ((Xml.Comment) misc).getText();
                            if (textComment.contains("TODO")) {
                                todos.add(textComment.trim());
                            }
                        }
                    }
                } else if (x instanceof Xml.Tag) {
                    Xml.Tag tag = ((Xml.Tag) v);
                    assert tag.getContent() != null;
                    for (Xml c : tag.getContent()) {
                        if (c instanceof Xml.Comment) {
                            String textComment = ((Xml.Comment) c).getText();
                            if (textComment.contains("TODO")) {
                                todos.add(textComment.trim());
                            }
                        }
                    }
                }
            }
            if (todos.isEmpty()) {
                return null;
            }
            return new TodoComment(cursor, todos);
        }
    }
}

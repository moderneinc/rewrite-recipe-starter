package com.yourorg;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class JacksonImportStyle extends NamedStyles {
    public JacksonImportStyle(UUID id, String name, String displayName, @Nullable String description, Set<String> tags, Collection<Style> styles) {
        super(id, name, displayName, description, tags, styles);
    }

    public static ImportLayoutStyle importLayout() {
        return ImportLayoutStyle.builder()
                .importAllOthers()
                .blankLine()
                .importPackage("com.fasterxml.jackson.core.*")
                .importPackage("tools.jackson.*")
                .blankLine()
                .importStaticAllOthers()
                .build();
    }

}

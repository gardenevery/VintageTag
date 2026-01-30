package com.gardenevery.vintagetag;

import javax.annotation.Nullable;

public enum TagType {
    ITEM("item"),
    FLUID("fluid"),
    BLOCK("block");

    private final String typeName;

    TagType(String typeName) {
        this.typeName = typeName;
    }

    public String getName() {
        return typeName;
    }

    @Nullable
    public static TagType getType(String typeName) {
        if (typeName == null) {
            return null;
        }

        return switch (typeName) {
            case "item" -> ITEM;
            case "fluid" -> FLUID;
            case "block" -> BLOCK;
            default -> null;
        };
    }
}

package com.gardenevery.vintagetag;

import javax.annotation.Nullable;

public enum TagType {
    ITEM("item"),
    FLUID("fluid"),
    BLOCK("block");

    TagType(String name) {}

    @Nullable
    public static TagType getType(String name) {
        return switch (name) {
            case "item" -> ITEM;
            case "fluid" -> FLUID;
            case "block" -> BLOCK;
            default -> null;
        };
    }
}

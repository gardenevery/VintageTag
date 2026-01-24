package com.gardenevery.vintagetag;

import javax.annotation.Nullable;

public enum TagType {
    ITEM("item"),
    FLUID("fluid"),
    BLOCK("block"),
    BLOCK_STATE("block_state");

    private final String name;

    TagType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public static TagType getType(String type) {
        if (type == null) {
            return null;
        }

        return switch (type.toLowerCase()) {
            case "item" -> ITEM;
            case "fluid" -> FLUID;
            case "block" -> BLOCK;
            case "block_state" -> BLOCK_STATE;
            default -> null;
        };
    }
}

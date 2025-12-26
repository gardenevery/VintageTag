package com.gardenevery.vintagetag;

public enum TagType {
    ITEM("item"),
    FLUID("fluid"),
    BLOCK("block");

    private final String name;

    TagType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static TagType getType(String name) {
        for (TagType type : TagType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}

package com.gardenevery.vintagetag;

import net.minecraft.block.Block;
import net.minecraftforge.fluids.Fluid;

final class TagManager {

    private static final Tag<ItemKey> ITEM = new Tag<>();
    private static final Tag<Fluid> FLUID = new Tag<>();
    private static final Tag<Block> BLOCK = new Tag<>();

    public static Tag<ItemKey> item() {
        return ITEM;
    }

    public static Tag<Fluid> fluid() {
        return FLUID;
    }

    public static Tag<Block> block() {
        return BLOCK;
    }
}

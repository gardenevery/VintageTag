package com.gardenevery.vintagetag;

import net.minecraft.block.Block;
import net.minecraftforge.fluids.Fluid;

final class TagManager {

    public static final Tag<ItemKey> ITEM = new Tag<>();
    public static final Tag<Fluid> FLUID = new Tag<>();
    public static final Tag<Block> BLOCK = new Tag<>();
}

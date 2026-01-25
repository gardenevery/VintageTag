package com.gardenevery.vintagetag;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;

import com.gardenevery.vintagetag.Tag.MutableTagContainer;

import net.minecraft.block.Block;
import net.minecraftforge.fluids.Fluid;

final class TagManager {

    private static final AtomicReference<Tag<ItemKey>> ITEM_SNAPSHOT = new AtomicReference<>(new Tag<>());
    private static final AtomicReference<Tag<Fluid>> FLUID_SNAPSHOT = new AtomicReference<>(new Tag<>());
    private static final AtomicReference<Tag<Block>> BLOCK_SNAPSHOT = new AtomicReference<>(new Tag<>());

    private static final MutableTagContainer<ItemKey> ITEM_CONTAINER = new MutableTagContainer<>();
    private static final MutableTagContainer<Fluid> FLUID_CONTAINER = new MutableTagContainer<>();
    private static final MutableTagContainer<Block> BLOCK_CONTAINER = new MutableTagContainer<>();

    @Nonnull
    public static Tag<ItemKey> item() {
        return ITEM_SNAPSHOT.get();
    }

    @Nonnull
    public static Tag<Fluid> fluid() {
        return FLUID_SNAPSHOT.get();
    }

    @Nonnull
    public static Tag<Block> block() {
        return BLOCK_SNAPSHOT.get();
    }

    public static void bake() {
        final Tag<ItemKey> newItemSnapshot = ITEM_CONTAINER.build();
        final Tag<Fluid> newFluidSnapshot = FLUID_CONTAINER.build();
        final Tag<Block> newBlockSnapshot = BLOCK_CONTAINER.build();

        synchronized (TagManager.class) {
            ITEM_SNAPSHOT.set(newItemSnapshot);
            FLUID_SNAPSHOT.set(newFluidSnapshot);
            BLOCK_SNAPSHOT.set(newBlockSnapshot);

            ITEM_CONTAINER.clear();
            FLUID_CONTAINER.clear();
            BLOCK_CONTAINER.clear();
        }
    }

    public static void registerItem(Set<ItemKey> itemKeys, String tagName) {
        ITEM_CONTAINER.register(itemKeys, tagName);
    }

    public static void registerFluid(Set<Fluid> fluids, String tagName) {
        FLUID_CONTAINER.register(fluids, tagName);
    }

    public static void registerBlock(Set<Block> blocks, String tagName) {
        BLOCK_CONTAINER.register(blocks, tagName);
    }

    public static void replaceItem(Set<ItemKey> itemKeys, String tagName) {
        ITEM_CONTAINER.replace(itemKeys, tagName);
    }

    public static void replaceFluid(Set<Fluid> fluids, String tagName) {
        FLUID_CONTAINER.replace(fluids, tagName);
    }

    public static void replaceBlock(Set<Block> blocks, String tagName) {
        BLOCK_CONTAINER.replace(blocks, tagName);
    }
}

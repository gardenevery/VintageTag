package com.gardenevery.vintagetag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.bsideup.jabel.Desugar;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Desugar
record ItemKey(Item item, int metadata) {
    @Nonnull
    public static ItemKey toKey(@Nonnull ItemStack stack) {
        int metadata = 0;
        if (stack.getHasSubtypes()) {
            metadata = stack.getMetadata();
        }
        return new ItemKey(stack.getItem(), metadata);
    }

    @Nonnull
    public static ObjectOpenHashSet<ItemKey> toKey(@Nullable ObjectOpenHashSet<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return new ObjectOpenHashSet<>();
        }

        ObjectOpenHashSet<ItemKey> keys = new ObjectOpenHashSet<>();
        for (var stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                var key = ItemKey.toKey(stack);
                keys.add(key);
            }
        }
        return keys;
    }

    @Nonnull
    public ItemStack toStack() {
        return new ItemStack(item, 1, metadata);
    }
}

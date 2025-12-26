package com.gardenevery.tag;

import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Desugar
record ItemKey(@Nonnull Item item, int metadata) {
    @Nonnull
    public static ItemKey toKey(@Nonnull ItemStack stack) {
        int metadata = 0;
        if (stack.getHasSubtypes()) {
            metadata = stack.getMetadata();
        }
        return new ItemKey(stack.getItem(), metadata);
    }

    @Nonnull
    public ItemStack toElement() {
        return new ItemStack(item, 1, metadata);
    }
}

package com.gardenevery.vintagetag;

import java.util.Objects;
import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Desugar
record ItemKey(Item item, int metadata) {

    public ItemKey {
        Objects.requireNonNull(item, "item must not be null");
    }

    @Nonnull
    public static ItemKey toKey(@Nonnull ItemStack stack) {
        int metadata = 0;
        if (stack.getHasSubtypes()) {
            metadata = stack.getMetadata();
        }
        return new ItemKey(stack.getItem(), metadata);
    }

    @Nonnull
    public ItemStack toStack() {
        return new ItemStack(item, 1, metadata);
    }
}

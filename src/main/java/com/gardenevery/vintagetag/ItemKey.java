package com.gardenevery.vintagetag;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    public static Set<ItemKey> toKeys(@Nullable Set<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return Collections.emptySet();
        }

        Set<ItemKey> keys = new HashSet<>();
        for (var stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                var key = ItemKey.toKey(stack);
                keys.add(key);
            }
        }
        return keys;
    }

    @Nonnull
    public ItemStack toElement() {
        return new ItemStack(item, 1, metadata);
    }
}

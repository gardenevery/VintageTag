package com.gardenevery.tag;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public final class TagHelper {

    private TagHelper() {}

    /**
     * Get all tags associated with an item
     */
    public static Set<String> tags(@Nullable ItemStack stack) {
        return (stack == null || stack.isEmpty()) ? Collections.emptySet() : TagManager.ITEM.getTag(ItemKey.toKey(stack));
    }

    /**
     * Get all tags associated with a fluid
     */
    public static Set<String> tags(@Nullable FluidStack stack) {
        return (stack == null || stack.getFluid() == null) ? Collections.emptySet() : TagManager.FLUID.getTag(stack.getFluid());
    }

    /**
     * Get all tags associated with a block
     */
    public static Set<String> tags(@Nullable Block block) {
        return block == null ? Collections.emptySet() : TagManager.BLOCK.getTag(block);
    }

    /**
     * Get all tags associated with a block state
     */
    public static Set<String> tags(@Nullable IBlockState blockState) {
        return blockState == null ? Collections.emptySet() : TagManager.BLOCK.getTag(blockState.getBlock());
    }

    /**
     * Get all tags associated with a tileentity
     */
    public static Set<String> tags(@Nullable TileEntity blockEntity) {
        return blockEntity == null ? Collections.emptySet() : TagManager.BLOCK.getTag(getBlock(blockEntity));
    }

    /**
     * Get all tags associated with a tag type
     */
    public static Set<String> allTags(@Nonnull TagType type) {
        return switch (type) {
            case ITEM -> TagManager.ITEM.getAllTag();
            case FLUID -> TagManager.FLUID.getAllTag();
            case BLOCK -> TagManager.BLOCK.getAllTag();
        };
    }

    /**
     * Get all item elements associated with a tag name
     */
    public static Set<ItemStack> itemElement(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return Collections.emptySet();
        }

        Set<ItemKey> keys = TagManager.ITEM.getKey(tagName);
        Set<ItemStack> result = new ObjectOpenHashSet<>();

        for (var key : keys) {
            result.add(key.toElement());
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Get all fluid elements associated with a tag name
     */
    public static Set<FluidStack> fluidElement(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return Collections.emptySet();
        }

        Set<Fluid> fluids = TagManager.FLUID.getKey(tagName);
        Set<FluidStack> result = new ObjectOpenHashSet<>();

        for (var fluid : fluids) {
            result.add(new FluidStack(fluid, 1000));
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Get all block elements associated with a tag name
     */
    public static Set<Block> blockElement(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return Collections.emptySet();
        }
        return TagManager.BLOCK.getKey(tagName);
    }

    /**
     * Get all items in the tag system (items that have at least one tag)
     */
    public static Set<ItemStack> allItemElement() {
        Set<ItemKey> keys = TagManager.ITEM.getAllKey();
        if (keys.isEmpty()) {
            return Collections.emptySet();
        }

        Set<ItemStack> result = new ObjectOpenHashSet<>();
        for (var key : keys) {
            result.add(key.toElement());
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Get all fluids in the tag system (fluids that have at least one tag)
     */
    public static Set<FluidStack> allFluidElement() {
        Set<Fluid> fluids = TagManager.FLUID.getAllKey();
        if (fluids.isEmpty()) {
            return Collections.emptySet();
        }

        Set<FluidStack> result = new ObjectOpenHashSet<>();
        for (var fluid : fluids) {
            result.add(new FluidStack(fluid, 1000));
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Get all blocks in the tag system (blocks that have at least one tag)
     */
    public static Set<Block> allBlockElement() {
        return TagManager.BLOCK.getAllKey();
    }

    /**
     * Check if an item has the specified tag
     */
    public static boolean hasTag(@Nullable String tagName, @Nullable ItemStack stack) {
        if (tagInvalid(tagName) || stack == null || stack.isEmpty()) {
            return false;
        }

        var key = ItemKey.toKey(stack);
        return TagManager.ITEM.hasTag(key, tagName);
    }

    /**
     * Check if a fluid has the specified tag
     */
    public static boolean hasTag(@Nullable String tagName, @Nullable FluidStack stack) {
        if (tagInvalid(tagName) || stack == null || stack.getFluid() == null) {
            return false;
        }
        return TagManager.FLUID.hasTag(stack.getFluid(), tagName);
    }

    /**
     * Check if a block has the specified tag
     */
    public static boolean hasTag(@Nullable String tagName, @Nullable Block block) {
        if (tagInvalid(tagName) || block == null) {
            return false;
        }
        return TagManager.BLOCK.hasTag(block, tagName);
    }

    /**
     * Check if a block state has the specified tag
     */
    public static boolean hasTag(@Nullable String tagName, @Nullable IBlockState blockState) {
        if (tagInvalid(tagName) || blockState == null) {
            return false;
        }
        return TagManager.BLOCK.hasTag(blockState.getBlock(), tagName);
    }

    /**
     * Check if a tileentity has the specified tag
     */
    public static boolean hasTag(@Nullable String tagName, @Nullable TileEntity blockEntity) {
        if (tagInvalid(tagName) || blockEntity == null) {
            return false;
        }
        return TagManager.BLOCK.hasTag(getBlock(blockEntity), tagName);
    }

    /**
     * Check if an item has any of the specified tags
     */
    public static boolean hasAnyTags(@Nullable Set<String> tagNames, @Nullable ItemStack stack) {
        if (tagInvalid(tagNames) || stack == null || stack.isEmpty()) {
            return false;
        }

        var key = ItemKey.toKey(stack);
        return TagManager.ITEM.hasAnyTag(key, tagNames);
    }

    /**
     * Check if a fluid has any of the specified tags
     */
    public static boolean hasAnyTags(@Nullable Set<String> tagNames, @Nullable FluidStack stack) {
        if (tagInvalid(tagNames) || stack == null ||stack.getFluid() == null) {
            return false;
        }
        return TagManager.FLUID.hasAnyTag(stack.getFluid(), tagNames);
    }

    /**
     * Check if a block has any of the specified tags
     */
    public static boolean hasAnyTags(@Nullable Set<String> tagNames, @Nullable Block block) {
        if (tagInvalid(tagNames) || block == null) {
            return false;
        }
        return TagManager.BLOCK.hasAnyTag(block, tagNames);
    }

    /**
     * Check if a blockState has any of the specified tags
     */
    public static boolean hasAnyTags(@Nullable Set<String> tagNames, @Nullable IBlockState blockState) {
        if (tagInvalid(tagNames) || blockState == null) {
            return false;
        }
        return TagManager.BLOCK.hasAnyTag(blockState.getBlock(), tagNames);
    }

    /**
     * Check if a tileentity has any of the specified tags
     */
    public static boolean hasAnyTags(@Nullable Set<String> tagNames, @Nullable TileEntity blockEntity) {
        if (tagInvalid(tagNames) || blockEntity == null) {
            return false;
        }
        return TagManager.BLOCK.hasAnyTag(getBlock(blockEntity), tagNames);
    }

    /**
     * Check if a tag exists for the specified type
     */
    public static boolean doesTagExist(@Nullable String tagName, @Nonnull TagType type) {
        if (tagInvalid(tagName)) {
            return false;
        }

        return switch (type) {
            case ITEM -> TagManager.ITEM.doesTagExist(tagName);
            case FLUID -> TagManager.FLUID.doesTagExist(tagName);
            case BLOCK -> TagManager.BLOCK.doesTagExist(tagName);
        };
    }

    /**
     * Check if a tag exists in any type
     */
    public static boolean doesTagExist(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return false;
        }
        return TagManager.ITEM.doesTagExist(tagName) || TagManager.FLUID.doesTagExist(tagName) || TagManager.BLOCK.doesTagExist(tagName);
    }

    /**
     * Check if an item exists in the tag system (has at least one tag)
     */
    public static boolean contains(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        var key = ItemKey.toKey(stack);
        return TagManager.ITEM.containsKey(key);
    }

    /**
     * Check if a fluid exists in the tag system (has at least one tag)
     */
    public static boolean contains(@Nullable FluidStack stack) {
        if (stack == null || stack.getFluid() == null) {
            return false;
        } else {
            return TagManager.FLUID.containsKey(stack.getFluid());
        }
    }

    /**
     * Check if a block exists in the tag system (has at least one tag)
     */
    public static boolean contains(@Nullable Block block) {
        if (block == null) {
            return false;
        }
        return TagManager.BLOCK.containsKey(block);
    }

    /**
     * Check if a block state exists in the tag system (has at least one tag)
     */
    public static boolean contains(@Nullable IBlockState blockState) {
        if (blockState == null) {
            return false;
        }
        return TagManager.BLOCK.containsKey(blockState.getBlock());
    }

    /**
     * Check if a block entity exists in the tag system (has at least one tag)
     */
    public static boolean contains(@Nullable TileEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        return TagManager.BLOCK.containsKey(getBlock(blockEntity));
    }

    /**
     * Get the total number of tags for the specified type
     */
    public static int tagCount(@Nonnull TagType type) {
        return switch (type) {
            case ITEM -> TagManager.ITEM.getTagCount();
            case FLUID -> TagManager.FLUID.getTagCount();
            case BLOCK -> TagManager.BLOCK.getTagCount();
        };
    }

    /**
     * Get the total number of tags across all types
     */
    public static int tagCount() {
        return TagManager.ITEM.getTagCount() + TagManager.FLUID.getTagCount() + TagManager.BLOCK.getTagCount();
    }

    /**
     * Get the total number of associations for the specified type
     * (sum of all keys across all tags)
     */
    public static int associations(@Nonnull TagType type) {
        return switch (type) {
            case ITEM -> TagManager.ITEM.getAssociations();
            case FLUID -> TagManager.FLUID.getAssociations();
            case BLOCK -> TagManager.BLOCK.getAssociations();
        };
    }

    /**
     * Get the total number of associations across all types
     */
    public static int associations() {
        return TagManager.ITEM.getAssociations() + TagManager.FLUID.getAssociations() + TagManager.BLOCK.getAssociations();
    }

    /**
     * Get the number of unique keys for the specified type
     * (count of distinct keys across all tags)
     */
    public static int keyCount(@Nonnull TagType type) {
        return switch (type) {
            case ITEM -> TagManager.ITEM.getKeyCount();
            case FLUID -> TagManager.FLUID.getKeyCount();
            case BLOCK -> TagManager.BLOCK.getKeyCount();
        };
    }

    /**
     * Get the total number of unique keys across all types
     */
    public static int keyCount() {
        return TagManager.ITEM.getKeyCount() + TagManager.FLUID.getKeyCount() + TagManager.BLOCK.getKeyCount();
    }

    /**
     * Clean the tag system (remove all item tags)
     */
    public static void cleanItemTag() {
        TagManager.ITEM.clean();
    }

    /**
     * Clean the tag system (remove all fluid tags)
     */
    public static void cleanFluidTag() {
        TagManager.FLUID.clean();
    }

    /**
     * Clean the tag system (remove all block tags)
     */
    public static void cleanBlockTag() {
        TagManager.BLOCK.clean();
    }

    /**
     * Clean the tag system (remove all tags)
     */
    public static void cleanAllTag() {
        TagManager.ITEM.clean();
        TagManager.FLUID.clean();
        TagManager.BLOCK.clean();
    }

    private static Block getBlock(@Nonnull TileEntity blockEntity) {
        return blockEntity.getWorld().getBlockState(blockEntity.getPos()).getBlock();
    }

    private static boolean tagInvalid(@Nullable String tagName) {
        return tagName == null || tagName.isEmpty();
    }

    private static boolean tagInvalid(@Nullable Set<String> tagNames) {
        return tagNames == null || tagNames.isEmpty();
    }
}

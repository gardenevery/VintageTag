package com.gardenevery.vintagetag;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class TagHelper {

    private TagHelper() {}

    private static final NonNullList<ItemStack> EMPTY = NonNullList.create();

    /**
     * Get all tags associated with the specified ItemStack
     *
     * @param stack The ItemStack to query, can be null
     * @return An unmodifiable set of tag names, empty if stack is null or empty
     */
    @Nonnull
    public static Set<String> tags(@Nullable ItemStack stack) {
        return (stack == null || stack.isEmpty()) ? Collections.emptySet() : TagManager.item().getTags(ItemKey.toKey(stack));
    }

    /**
     * Get all tags associated with the specified FluidStack
     *
     * @param stack The FluidStack to query, can be null
     * @return An unmodifiable set of tag names, empty if stack is null or has no fluid
     */
    @Nonnull
    public static Set<String> tags(@Nullable FluidStack stack) {
        return (stack == null || stack.getFluid() == null) ? Collections.emptySet() : TagManager.fluid().getTags(stack.getFluid());
    }

    /**
     * Get all tags associated with the specified Block
     *
     * @param block The Block to query, can be null
     * @return An unmodifiable set of tag names, empty if block is null
     */
    @Nonnull
    public static Set<String> tags(@Nullable Block block) {
        return block == null ? Collections.emptySet() : TagManager.block().getTags(block);
    }

    /**
     * Get all tags associated with the specified BlockState
     *
     * @param blockState The IBlockState to query, can be null
     * @return An unmodifiable set of tag names, empty if blockState is null
     */
    @Nonnull
    public static Set<String> tags(@Nullable IBlockState blockState) {
        return blockState == null ? Collections.emptySet() : TagManager.block().getTags(blockState.getBlock());
    }

    /**
     * Get all tags associated with the specified TileEntity
     *
     * @param blockEntity The TileEntity to query, can be null
     * @return An unmodifiable set of tag names, empty if blockEntity is null
     */
    @Nonnull
    public static Set<String> tags(@Nullable TileEntity blockEntity) {
        return blockEntity == null ? Collections.emptySet() : TagManager.block().getTags(getBlock(blockEntity));
    }

    /**
     * Get all tags defined for items
     *
     * @return An unmodifiable set of all item tag names
     */
    @Nonnull
    public static Set<String> allItemTags() {
        return TagManager.item().allTags();
    }

    /**
     * Get all tags defined for fluids
     *
     * @return An unmodifiable set of all fluid tag names
     */
    @Nonnull
    public static Set<String> allFluidTags() {
        return TagManager.fluid().allTags();
    }

    /**
     * Get all tags defined for blocks
     *
     * @return An unmodifiable set of all block tag names
     */
    @Nonnull
    public static Set<String> allBlockTags() {
        return TagManager.block().allTags();
    }

    /**
     * Get all ItemStacks associated with the specified tag name
     *
     * @param tagName The tag name to query, can be null
     * @return An unmodifiable set of ItemStacks, empty if tagName is null or empty
     */
    @Nonnull
    public static Set<ItemStack> itemKeys(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return Collections.emptySet();
        }

        Set<ItemKey> keys = TagManager.item().getKeys(tagName);
        Set<ItemStack> result = new ObjectOpenHashSet<>();

        for (var key : keys) {
            result.add(key.toElement());
        }
        return result;
    }

    /**
     * Get all ItemStacks associated with the specified tag name as a NonNullList
     *
     * @param tagName The tag name to query, can be null
     * @return A NonNullList of ItemStacks, empty if tagName is null or empty
     */
    @Nonnull
    public static NonNullList<ItemStack> itemKeysList(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return EMPTY;
        }

        Set<ItemKey> keys = TagManager.item().getKeys(tagName);
        NonNullList<ItemStack> result = NonNullList.create();

        for (var key : keys) {
            result.add(key.toElement());
        }
        return result;
    }

    /**
     * Get all FluidStacks associated with the specified tag name
     *
     * @param tagName The tag name to query, can be null
     * @return An unmodifiable set of FluidStacks (1000mb each), empty if tagName is null or empty
     */
    @Nonnull
    public static Set<FluidStack> fluidKeys(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return Collections.emptySet();
        }

        Set<Fluid> keys = TagManager.fluid().getKeys(tagName);
        Set<FluidStack> result = new ObjectOpenHashSet<>();

        for (var key : keys) {
            result.add(new FluidStack(key, 1000));
        }
        return result;
    }

    /**
     * Get all Blocks associated with the specified tag name
     *
     * @param tagName The tag name to query, can be null
     * @return An unmodifiable set of Blocks, empty if tagName is null or empty
     */
    @Nonnull
    public static Set<Block> blockKeys(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return Collections.emptySet();
        }
        return TagManager.block().getKeys(tagName);
    }

    /**
     * Get all ItemStacks that have at least one tag
     *
     * @return An unmodifiable set of all tagged ItemStacks
     */
    @Nonnull
    public static Set<ItemStack> allItemKeys() {
        Set<ItemKey> keys = TagManager.item().allKeys();
        Set<ItemStack> result = new ObjectOpenHashSet<>();

        for (var key : keys) {
            result.add(key.toElement());
        }
        return result;
    }

    /**
     * Get all FluidStacks that have at least one tag
     *
     * @return An unmodifiable set of all tagged FluidStacks (1000mb each)
     */
    @Nonnull
    public static Set<FluidStack> allFluidKeys() {
        Set<Fluid> keys = TagManager.fluid().allKeys();
        Set<FluidStack> result = new ObjectOpenHashSet<>();

        for (var key : keys) {
            result.add(new FluidStack(key, 1000));
        }
        return result;
    }

    /**
     * Get all Blocks that have at least one tag
     *
     * @return An unmodifiable set of all tagged Blocks
     */
    @Nonnull
    public static Set<Block> allBlockKeys() {
        return TagManager.block().allKeys();
    }

    /**
     * Get all item tag entries with their associated ItemStacks
     *
     * @return An unmodifiable map of tag name to set of ItemStacks
     */
    @Nonnull
    public static Map<String, Set<ItemStack>> allItemEntries() {
        Map<String, Set<ItemKey>> itemKeyMap = TagManager.item().getAllEntries();
        Map<String, Set<ItemStack>> result = new HashMap<>();

        for (Map.Entry<String, Set<ItemKey>> entry : itemKeyMap.entrySet()) {
            Set<ItemStack> itemStacks = entry.getValue().stream()
                    .map(ItemKey::toElement)
                    .collect(Collectors.toSet());
            result.put(entry.getKey(), itemStacks);
        }
        return result;
    }

    /**
     * Get all fluid tag entries with their associated FluidStacks
     *
     * @return An unmodifiable map of tag name to set of FluidStacks (1000mb each)
     */
    @Nonnull
    public static Map<String, Set<FluidStack>> allFluidEntries() {
        Map<String, Set<Fluid>> fluidMap = TagManager.fluid().getAllEntries();
        Map<String, Set<FluidStack>> result = new HashMap<>();

        for (Map.Entry<String, Set<Fluid>> entry : fluidMap.entrySet()) {
            Set<FluidStack> fluidStacks = entry.getValue().stream()
                    .map(fluid -> new FluidStack(fluid, 1000))
                    .collect(Collectors.toSet());
            result.put(entry.getKey(), fluidStacks);
        }
        return result;
    }

    /**
     * Get all block tag entries with their associated Blocks
     *
     * @return An unmodifiable map of tag name to set of Blocks
     */
    @Nonnull
    public static Map<String, Set<Block>> allBlockEntries() {
        return TagManager.block().getAllEntries();
    }

    /**
     * Check if the specified ItemStack has the given tag
     *
     * @param stack The ItemStack to check, can be null
     * @param tagName The tag name to check for, can be null
     * @return true if stack is not null/empty, tagName is valid, and stack has the tag
     */
    public static boolean hasTag(@Nullable ItemStack stack, @Nullable String tagName) {
        if (stack == null || stack.isEmpty() || tagInvalid(tagName)) {
            return false;
        }

        var key = ItemKey.toKey(stack);
        return TagManager.item().hasTag(key, tagName);
    }

    /**
     * Check if the specified FluidStack has the given tag
     *
     * @param stack The FluidStack to check, can be null
     * @param tagName The tag name to check for, can be null
     * @return true if stack is not null/has fluid, tagName is valid, and fluid has the tag
     */
    public static boolean hasTag(@Nullable FluidStack stack, @Nullable String tagName) {
        if (stack == null || stack.getFluid() == null || tagInvalid(tagName)) {
            return false;
        }
        return TagManager.fluid().hasTag(stack.getFluid(), tagName);
    }

    /**
     * Check if the specified Block has the given tag
     *
     * @param block The Block to check, can be null
     * @param tagName The tag name to check for, can be null
     * @return true if block is not null, tagName is valid, and block has the tag
     */
    public static boolean hasTag(@Nullable Block block, @Nullable String tagName) {
        if (block == null || tagInvalid(tagName)) {
            return false;
        }
        return TagManager.block().hasTag(block, tagName);
    }

    /**
     * Check if the specified BlockState has the given tag
     *
     * @param blockState The IBlockState to check, can be null
     * @param tagName The tag name to check for, can be null
     * @return true if blockState is not null, tagName is valid, and block has the tag
     */
    public static boolean hasTag(@Nullable IBlockState blockState, @Nullable String tagName) {
        if (blockState == null || tagInvalid(tagName)) {
            return false;
        }
        return TagManager.block().hasTag(blockState.getBlock(), tagName);
    }

    /**
     * Check if the specified TileEntity's block has the given tag
     *
     * @param blockEntity The TileEntity to check, can be null
     * @param tagName The tag name to check for, can be null
     * @return true if blockEntity is not null, tagName is valid, and block has the tag
     */
    public static boolean hasTag(@Nullable TileEntity blockEntity, @Nullable String tagName) {
        if (blockEntity == null || tagInvalid(tagName)) {
            return false;
        }
        return TagManager.block().hasTag(getBlock(blockEntity), tagName);
    }

    /**
     * Check if the specified ItemStack has any of the given tags
     *
     * @param stack The ItemStack to check, can be null
     * @param tagNames The tag names to check for, can be null or empty
     * @return true if stack is not null/empty, tagNames are valid, and stack has any of the tags
     */
    public static boolean hasAnyTag(@Nullable ItemStack stack, @Nullable String... tagNames) {
        if (stack == null || stack.isEmpty() || tagInvalid(tagNames)) {
            return false;
        }

        var key = ItemKey.toKey(stack);
        return TagManager.item().hasAnyTag(key, tagNames);
    }

    /**
     * Check if the specified FluidStack has any of the given tags
     *
     * @param stack The FluidStack to check, can be null
     * @param tagNames The tag names to check for, can be null or empty
     * @return true if stack is not null/has fluid, tagNames are valid, and fluid has any of the tags
     */
    public static boolean hasAnyTag(@Nullable FluidStack stack, @Nullable String... tagNames) {
        if (stack == null ||stack.getFluid() == null || tagInvalid(tagNames)) {
            return false;
        }
        return TagManager.fluid().hasAnyTag(stack.getFluid(), tagNames);
    }

    /**
     * Check if the specified Block has any of the given tags
     *
     * @param block The Block to check, can be null
     * @param tagNames The tag names to check for, can be null or empty
     * @return true if block is not null, tagNames are valid, and block has any of the tags
     */
    public static boolean hasAnyTag(@Nullable Block block, @Nullable String... tagNames) {
        if (block == null || tagInvalid(tagNames)) {
            return false;
        }
        return TagManager.block().hasAnyTag(block, tagNames);
    }

    /**
     * Check if the specified BlockState has any of the given tags
     *
     * @param blockState The IBlockState to check, can be null
     * @param tagNames The tag names to check for, can be null or empty
     * @return true if blockState is not null, tagNames are valid, and block has any of the tags
     */
    public static boolean hasAnyTag(@Nullable IBlockState blockState, @Nullable String... tagNames) {
        if (blockState == null || tagInvalid(tagNames)) {
            return false;
        }
        return TagManager.block().hasAnyTag(blockState.getBlock(), tagNames);
    }

    /**
     * Check if the specified TileEntity's block has any of the given tags
     *
     * @param blockEntity The TileEntity to check, can be null
     * @param tagNames The tag names to check for, can be null or empty
     * @return true if blockEntity is not null, tagNames are valid, and block has any of the tags
     */
    public static boolean hasAnyTag(@Nullable TileEntity blockEntity, @Nullable String... tagNames) {
        if (blockEntity == null || tagInvalid(tagNames)) {
            return false;
        }
        return TagManager.block().hasAnyTag(getBlock(blockEntity), tagNames);
    }

    /**
     * Get the total number of tags defined for items
     *
     * @return The count of unique item tags
     */
    public static int itemTagCount() {
        return TagManager.item().tagCount();
    }

    /**
     * Get the total number of tags defined for fluids
     *
     * @return The count of unique fluid tags
     */
    public static int fluidTagCount() {
        return TagManager.fluid().tagCount();
    }

    /**
     * Get the total number of tags defined for blocks
     *
     * @return The count of unique block tags
     */
    public static int blockTagCount() {
        return TagManager.block().tagCount();
    }

    /**
     * Get the total number of items that have at least one tag
     *
     * @return The count of unique tagged items
     */
    public static int itemKeyCount() {
        return TagManager.item().keyCount();
    }

    /**
     * Get the total number of fluids that have at least one tag
     *
     * @return The count of unique tagged fluids
     */
    public static int fluidKeyCount() {
        return TagManager.fluid().keyCount();
    }

    /**
     * Get the total number of blocks that have at least one tag
     *
     * @return The count of unique tagged blocks
     */
    public static int blockKeyCount() {
        return TagManager.block().keyCount();
    }

    /**
     * Get the total number of item-tag associations
     *
     * @return The count of all item-tag pairs
     */
    public static int itemAssociationCount() {
        return TagManager.item().associationCount();
    }

    /**
     * Get the total number of fluid-tag associations
     *
     * @return The count of all fluid-tag pairs
     */
    public static int fluidAssociationCount() {
        return TagManager.fluid().associationCount();
    }

    /**
     * Get the total number of block-tag associations
     *
     * @return The count of all block-tag pairs
     */
    public static int blockAssociationCount() {
        return TagManager.block().associationCount();
    }

    /**
     * Check if the specified tag name exists for items
     *
     * @param tagName The tag name to check, can be null
     * @return true if tagName is valid and exists in item tags
     */
    public static boolean itemTagExists(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return false;
        }
        return TagManager.item().exists(tagName);
    }

    /**
     * Check if the specified tag name exists for fluids
     *
     * @param tagName The tag name to check, can be null
     * @return true if tagName is valid and exists in fluid tags
     */
    public static boolean fluidTagExists(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return false;
        }
        return TagManager.fluid().exists(tagName);
    }

    /**
     * Check if the specified tag name exists for blocks
     *
     * @param tagName The tag name to check, can be null
     * @return true if tagName is valid and exists in block tags
     */
    public static boolean blockTagExists(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return false;
        }
        return TagManager.block().exists(tagName);
    }

    /**
     * Check if the specified tag name exists in any category (items, fluids, or blocks)
     *
     * @param tagName The tag name to check, can be null
     * @return true if tagName is valid and exists in any tag category
     */
    public static boolean tagExists(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return false;
        }
        return TagManager.item().exists(tagName) || TagManager.fluid().exists(tagName) || TagManager.block().exists(tagName);
    }

    private static Block getBlock(@Nonnull TileEntity blockEntity) {
        return blockEntity.getWorld().getBlockState(blockEntity.getPos()).getBlock();
    }

    private static boolean tagInvalid(@Nullable String tagName) {
        return tagName == null || tagName.isEmpty();
    }

    private static boolean tagInvalid(@Nullable String... tagNames) {
        return tagNames == null || tagNames.length == 0;
    }
}

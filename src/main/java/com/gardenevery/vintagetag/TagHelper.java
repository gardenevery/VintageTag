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
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class TagHelper {

    private TagHelper() {}

    @Nonnull
    public static Set<String> tags(@Nullable ItemStack stack) {
        return (stack == null || stack.isEmpty()) ? Collections.emptySet() : TagManager.ITEM.getTags(ItemKey.toKey(stack));
    }

    @Nonnull
    public static Set<String> tags(@Nullable FluidStack stack) {
        return (stack == null || stack.getFluid() == null) ? Collections.emptySet() : TagManager.FLUID.getTags(stack.getFluid());
    }

    @Nonnull
    public static Set<String> tags(@Nullable Block block) {
        return block == null ? Collections.emptySet() : TagManager.BLOCK.getTags(block);
    }

    @Nonnull
    public static Set<String> tags(@Nullable IBlockState blockState) {
        return blockState == null ? Collections.emptySet() : TagManager.BLOCK.getTags(blockState.getBlock());
    }

    @Nonnull
    public static Set<String> tags(@Nullable TileEntity blockEntity) {
        return blockEntity == null ? Collections.emptySet() : TagManager.BLOCK.getTags(getBlock(blockEntity));
    }

    @Nonnull
    public static Set<String> allItemTags() {
        return TagManager.ITEM.allTags();
    }

    @Nonnull
    public static Set<String> allFluidTags() {
        return TagManager.FLUID.allTags();
    }

    @Nonnull
    public static Set<String> allBlockTags() {
        return TagManager.BLOCK.allTags();
    }

    @Nonnull
    public static Set<ItemStack> itemKeys(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return Collections.emptySet();
        }

        Set<ItemKey> keys = TagManager.ITEM.getKeys(tagName);
        Set<ItemStack> result = new ObjectOpenHashSet<>();

        for (var key : keys) {
            result.add(key.toElement());
        }
        return Collections.unmodifiableSet(result);
    }

    @Nonnull
    public static Set<FluidStack> fluidKeys(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return Collections.emptySet();
        }

        Set<Fluid> keys = TagManager.FLUID.getKeys(tagName);
        Set<FluidStack> result = new ObjectOpenHashSet<>();

        for (var key : keys) {
            result.add(new FluidStack(key, 1000));
        }
        return Collections.unmodifiableSet(result);
    }

    @Nonnull
    public static Set<Block> blockKeys(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return Collections.emptySet();
        }
        return TagManager.BLOCK.getKeys(tagName);
    }

    @Nonnull
    public static Set<ItemStack> allItemKeys() {
        Set<ItemKey> keys = TagManager.ITEM.allKeys();
        Set<ItemStack> result = new ObjectOpenHashSet<>();

        for (var key : keys) {
            result.add(key.toElement());
        }
        return Collections.unmodifiableSet(result);
    }

    @Nonnull
    public static Set<FluidStack> allFluidKeys() {
        Set<Fluid> keys = TagManager.FLUID.allKeys();
        Set<FluidStack> result = new ObjectOpenHashSet<>();

        for (var key : keys) {
            result.add(new FluidStack(key, 1000));
        }
        return Collections.unmodifiableSet(result);
    }

    @Nonnull
    public static Set<Block> allBlockKeys() {
        return TagManager.BLOCK.allKeys();
    }

    @Nonnull
    public static Map<String, Set<ItemStack>> allItemEntries() {
        Map<String, Set<ItemKey>> itemKeyMap = TagManager.ITEM.getAllEntries();
        Map<String, Set<ItemStack>> result = new HashMap<>();

        for (Map.Entry<String, Set<ItemKey>> entry : itemKeyMap.entrySet()) {
            Set<ItemStack> itemStacks = entry.getValue().stream()
                    .map(ItemKey::toElement)
                    .collect(Collectors.toSet());
            result.put(entry.getKey(), itemStacks);
        }

        return result;
    }

    @Nonnull
    public static Map<String, Set<FluidStack>> allFluidEntries() {
        Map<String, Set<Fluid>> fluidMap = TagManager.FLUID.getAllEntries();
        Map<String, Set<FluidStack>> result = new HashMap<>();

        for (Map.Entry<String, Set<Fluid>> entry : fluidMap.entrySet()) {
            Set<FluidStack> fluidStacks = entry.getValue().stream()
                    .map(fluid -> new FluidStack(fluid, 1000))
                    .collect(Collectors.toSet());
            result.put(entry.getKey(), fluidStacks);
        }
        return result;
    }

    @Nonnull
    public static Map<String, Set<Block>> allBlockEntries() {
        return TagManager.BLOCK.getAllEntries();
    }

    public static boolean hasTag(@Nullable ItemStack stack, @Nullable String tagName) {
        if (stack == null || stack.isEmpty() || tagInvalid(tagName)) {
            return false;
        }

        var key = ItemKey.toKey(stack);
        return TagManager.ITEM.hasTag(key, tagName);
    }

    public static boolean hasTag(@Nullable FluidStack stack, @Nullable String tagName) {
        if (stack == null || stack.getFluid() == null || tagInvalid(tagName)) {
            return false;
        }
        return TagManager.FLUID.hasTag(stack.getFluid(), tagName);
    }

    public static boolean hasTag(@Nullable Block block, @Nullable String tagName) {
        if (block == null || tagInvalid(tagName)) {
            return false;
        }
        return TagManager.BLOCK.hasTag(block, tagName);
    }

    public static boolean hasTag(@Nullable IBlockState blockState, @Nullable String tagName) {
        if (blockState == null || tagInvalid(tagName)) {
            return false;
        }
        return TagManager.BLOCK.hasTag(blockState.getBlock(), tagName);
    }

    public static boolean hasTag(@Nullable TileEntity blockEntity, @Nullable String tagName) {
        if (blockEntity == null || tagInvalid(tagName)) {
            return false;
        }
        return TagManager.BLOCK.hasTag(getBlock(blockEntity), tagName);
    }

    public static boolean hasAnyTag(@Nullable ItemStack stack, @Nullable String... tagNames) {
        if (stack == null || stack.isEmpty() || tagInvalid(tagNames)) {
            return false;
        }

        var key = ItemKey.toKey(stack);
        return TagManager.ITEM.hasAnyTag(key, tagNames);
    }

    public static boolean hasAnyTag(@Nullable FluidStack stack, @Nullable String... tagNames) {
        if (stack == null ||stack.getFluid() == null || tagInvalid(tagNames)) {
            return false;
        }
        return TagManager.FLUID.hasAnyTag(stack.getFluid(), tagNames);
    }

    public static boolean hasAnyTag(@Nullable Block block, @Nullable String... tagNames) {
        if (block == null || tagInvalid(tagNames)) {
            return false;
        }
        return TagManager.BLOCK.hasAnyTag(block, tagNames);
    }

    public static boolean hasAnyTag(@Nullable IBlockState blockState, @Nullable String... tagNames) {
        if (blockState == null || tagInvalid(tagNames)) {
            return false;
        }
        return TagManager.BLOCK.hasAnyTag(blockState.getBlock(), tagNames);
    }

    public static boolean hasAnyTag(@Nullable TileEntity blockEntity, @Nullable String... tagNames) {
        if (blockEntity == null || tagInvalid(tagNames)) {
            return false;
        }
        return TagManager.BLOCK.hasAnyTag(getBlock(blockEntity), tagNames);
    }

    public static int itemTagCount() {
        return TagManager.ITEM.tagCount();
    }

    public static int fluidTagCount() {
        return TagManager.FLUID.tagCount();
    }

    public static int blockTagCount() {
        return TagManager.BLOCK.tagCount();
    }

    public static int itemKeyCount() {
        return TagManager.ITEM.keyCount();
    }

    public static int fluidKeyCount() {
        return TagManager.FLUID.keyCount();
    }

    public static int blockKeyCount() {
        return TagManager.BLOCK.keyCount();
    }

    public static int itemAssociationCount() {
        return TagManager.ITEM.associationCount();
    }

    public static int fluidAssociationCount() {
        return TagManager.FLUID.associationCount();
    }

    public static int blockAssociationCount() {
        return TagManager.BLOCK.associationCount();
    }

    public boolean itemTagExists(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return false;
        }
        return TagManager.ITEM.exists(tagName);
    }

    public boolean fluidTagExists(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return false;
        }
        return TagManager.FLUID.exists(tagName);
    }

    public boolean blockTagExists(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return false;
        }
        return TagManager.BLOCK.exists(tagName);
    }

    public boolean tagExists(@Nullable String tagName) {
        if (tagInvalid(tagName)) {
            return false;
        }
        return TagManager.ITEM.exists(tagName) || TagManager.FLUID.exists(tagName) || TagManager.BLOCK.exists(tagName);
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

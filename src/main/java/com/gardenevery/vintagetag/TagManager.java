package com.gardenevery.vintagetag;

import java.util.Set;
import javax.annotation.Nonnull;

import com.gardenevery.vintagetag.Tag.MutableTagContainer;

import net.minecraft.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;

final class TagManager {
	private static volatile Tag<ItemKey> ITEM_TAG_SNAPSHOT = new Tag<>();
	private static volatile Tag<Fluid> FLUID_TAG_SNAPSHOT = new Tag<>();
	private static volatile Tag<Block> BLOCK_TAG_SNAPSHOT = new Tag<>();

	private static final MutableTagContainer<ItemKey> ITEM_TAG_CONTAINER = new MutableTagContainer<>();
	private static final MutableTagContainer<Fluid> FLUID_TAG_CONTAINER = new MutableTagContainer<>();
	private static final MutableTagContainer<Block> BLOCK_TAG_CONTAINER = new MutableTagContainer<>();

	private static final MutableTagContainer<ItemKey> ORE_DICT_TAG_CACHE = new MutableTagContainer<>();
	private static final MutableTagContainer<ItemKey> ORE_DICT_AND_MOD_TAG_CACHE = new MutableTagContainer<>();
	private static final MutableTagContainer<ItemKey> MOD_ITEM_TAG_CACHE = new MutableTagContainer<>();
	private static final MutableTagContainer<Fluid> MOD_FLUID_TAG_CACHE = new MutableTagContainer<>();
	private static final MutableTagContainer<Block> MOD_BLOCK_TAG_CACHE = new MutableTagContainer<>();

	@Nonnull
	public static Tag<ItemKey> item() {
		return ITEM_TAG_SNAPSHOT;
	}

	@Nonnull
	public static Tag<Fluid> fluid() {
		return FLUID_TAG_SNAPSHOT;
	}

	@Nonnull
	public static Tag<Block> block() {
		return BLOCK_TAG_SNAPSHOT;
	}

	public static void copyOreTags() {
		ORE_DICT_TAG_CACHE.copyFrom(ITEM_TAG_CONTAINER);
	}

	public static void copyModTags() {
		MOD_ITEM_TAG_CACHE.copyFrom(ITEM_TAG_CONTAINER);
		MOD_FLUID_TAG_CACHE.copyFrom(FLUID_TAG_CONTAINER);
		MOD_BLOCK_TAG_CACHE.copyFrom(BLOCK_TAG_CONTAINER);
	}

	public static void copyOreAndModTags() {
		ORE_DICT_AND_MOD_TAG_CACHE.copyFrom(ITEM_TAG_CONTAINER);
	}

	public static void applyOreTags() {
		ITEM_TAG_CONTAINER.copyFrom(ORE_DICT_TAG_CACHE);
	}

	public static void applyModTags() {
		ITEM_TAG_CONTAINER.copyFrom(MOD_ITEM_TAG_CACHE);
		FLUID_TAG_CONTAINER.copyFrom(MOD_FLUID_TAG_CACHE);
		BLOCK_TAG_CONTAINER.copyFrom(MOD_BLOCK_TAG_CACHE);
	}

	public static void applyOreAndModTags() {
		ITEM_TAG_CONTAINER.copyFrom(ORE_DICT_AND_MOD_TAG_CACHE);
		FLUID_TAG_CONTAINER.copyFrom(MOD_FLUID_TAG_CACHE);
		BLOCK_TAG_CONTAINER.copyFrom(MOD_BLOCK_TAG_CACHE);
	}

	public static void clear() {
		ITEM_TAG_CONTAINER.clear();
		FLUID_TAG_CONTAINER.clear();
		BLOCK_TAG_CONTAINER.clear();
	}

	public static void bake() {
		final var newItemSnapshot = ITEM_TAG_CONTAINER.build();
		final var newFluidSnapshot = FLUID_TAG_CONTAINER.build();
		final var newBlockSnapshot = BLOCK_TAG_CONTAINER.build();

		ITEM_TAG_SNAPSHOT = newItemSnapshot;
		FLUID_TAG_SNAPSHOT = newFluidSnapshot;
		BLOCK_TAG_SNAPSHOT = newBlockSnapshot;

		ITEM_TAG_CONTAINER.clear();
		FLUID_TAG_CONTAINER.clear();
		BLOCK_TAG_CONTAINER.clear();

		MinecraftForge.EVENT_BUS.post(new TagEvent());
	}

	public static void registerItem(@Nonnull Set<ItemKey> itemKeys, @Nonnull String tagName) {
		ITEM_TAG_CONTAINER.register(itemKeys, tagName);
	}

	public static void registerItem(@Nonnull Set<ItemKey> itemKeys, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		ITEM_TAG_CONTAINER.register(itemKeys, tagName, tagInclude);
	}

	public static void registerFluid(@Nonnull Set<Fluid> fluids, @Nonnull String tagName) {
		FLUID_TAG_CONTAINER.register(fluids, tagName);
	}

	public static void registerFluid(@Nonnull Set<Fluid> fluids, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		FLUID_TAG_CONTAINER.register(fluids, tagName, tagInclude);
	}

	public static void registerBlock(@Nonnull Set<Block> blocks, @Nonnull String tagName) {
		BLOCK_TAG_CONTAINER.register(blocks, tagName);
	}

	public static void registerBlock(@Nonnull Set<Block> blocks, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		BLOCK_TAG_CONTAINER.register(blocks, tagName, tagInclude);
	}

	public static void replaceItem(@Nonnull Set<ItemKey> itemKeys, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		ITEM_TAG_CONTAINER.replace(itemKeys, tagName, tagInclude);
	}

	public static void replaceFluid(@Nonnull Set<Fluid> fluids, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		FLUID_TAG_CONTAINER.replace(fluids, tagName, tagInclude);
	}

	public static void replaceBlock(@Nonnull Set<Block> blocks, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		BLOCK_TAG_CONTAINER.replace(blocks, tagName, tagInclude);
	}
}

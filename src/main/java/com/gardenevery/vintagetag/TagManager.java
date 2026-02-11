package com.gardenevery.vintagetag;

import java.util.Set;
import javax.annotation.Nonnull;

import com.gardenevery.vintagetag.Tag.MutableTagContainer;
import com.gardenevery.vintagetag.TagEntry.BlockEntry;
import com.gardenevery.vintagetag.TagEntry.FluidEntry;
import com.gardenevery.vintagetag.TagEntry.ItemEntry;

import net.minecraftforge.common.MinecraftForge;

final class TagManager {
	private static volatile Tag<ItemEntry> ITEM_TAG_SNAPSHOT = new Tag<>();
	private static volatile Tag<FluidEntry> FLUID_TAG_SNAPSHOT = new Tag<>();
	private static volatile Tag<BlockEntry> BLOCK_TAG_SNAPSHOT = new Tag<>();

	private static final MutableTagContainer<ItemEntry> ITEM_TAG_CONTAINER = new MutableTagContainer<>();
	private static final MutableTagContainer<FluidEntry> FLUID_TAG_CONTAINER = new MutableTagContainer<>();
	private static final MutableTagContainer<BlockEntry> BLOCK_TAG_CONTAINER = new MutableTagContainer<>();

	@Nonnull
	public static Tag<ItemEntry> item() {
		return ITEM_TAG_SNAPSHOT;
	}

	@Nonnull
	public static Tag<FluidEntry> fluid() {
		return FLUID_TAG_SNAPSHOT;
	}

	@Nonnull
	public static Tag<BlockEntry> block() {
		return BLOCK_TAG_SNAPSHOT;
	}

	public static void registerItem(@Nonnull Set<ItemEntry> entry, @Nonnull String tagName) {
		ITEM_TAG_CONTAINER.register(entry, tagName);
	}

	public static void registerFluid(@Nonnull Set<FluidEntry> entry, @Nonnull String tagName) {
		FLUID_TAG_CONTAINER.register(entry, tagName);
	}

	public static void registerBlock(@Nonnull Set<BlockEntry> entry, @Nonnull String tagName) {
		BLOCK_TAG_CONTAINER.register(entry, tagName);
	}

	public static void replaceItem(@Nonnull Set<ItemEntry> entry, @Nonnull String tagName) {
		ITEM_TAG_CONTAINER.replace(entry, tagName);
	}

	public static void replaceFluid(@Nonnull Set<FluidEntry> entry, @Nonnull String tagName) {
		FLUID_TAG_CONTAINER.replace(entry, tagName);
	}

	public static void replaceBlock(@Nonnull Set<BlockEntry> entry, @Nonnull String tagName) {
		BLOCK_TAG_CONTAINER.replace(entry, tagName);
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
}

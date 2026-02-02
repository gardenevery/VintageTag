package com.gardenevery.vintagetag;

import java.util.Set;
import javax.annotation.Nonnull;

import com.gardenevery.vintagetag.Tag.MutableTagContainer;

import net.minecraft.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;

final class TagManager {
	private static volatile Tag<ItemKey> ITEM_SNAPSHOT = new Tag<>();
	private static volatile Tag<Fluid> FLUID_SNAPSHOT = new Tag<>();
	private static volatile Tag<Block> BLOCK_SNAPSHOT = new Tag<>();

	private static final MutableTagContainer<ItemKey> ITEM_CONTAINER = new MutableTagContainer<>();
	private static final MutableTagContainer<Fluid> FLUID_CONTAINER = new MutableTagContainer<>();
	private static final MutableTagContainer<Block> BLOCK_CONTAINER = new MutableTagContainer<>();

	@Nonnull
	public static Tag<ItemKey> item() {
		return ITEM_SNAPSHOT;
	}

	@Nonnull
	public static Tag<Fluid> fluid() {
		return FLUID_SNAPSHOT;
	}

	@Nonnull
	public static Tag<Block> block() {
		return BLOCK_SNAPSHOT;
	}

	public static void bake() {
		final var newItemSnapshot = ITEM_CONTAINER.build();
		final var newFluidSnapshot = FLUID_CONTAINER.build();
		final var newBlockSnapshot = BLOCK_CONTAINER.build();

		ITEM_SNAPSHOT = newItemSnapshot;
		FLUID_SNAPSHOT = newFluidSnapshot;
		BLOCK_SNAPSHOT = newBlockSnapshot;

		ITEM_CONTAINER.clear();
		FLUID_CONTAINER.clear();
		BLOCK_CONTAINER.clear();

		MinecraftForge.EVENT_BUS.post(new TagEvent());
	}

	public static void registerItem(@Nonnull Set<ItemKey> itemKeys, @Nonnull String tagName) {
		ITEM_CONTAINER.register(itemKeys, tagName);
	}

	public static void registerItem(@Nonnull Set<ItemKey> itemKeys, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		ITEM_CONTAINER.register(itemKeys, tagName, tagInclude);
	}

	public static void registerFluid(@Nonnull Set<Fluid> fluids, @Nonnull String tagName) {
		FLUID_CONTAINER.register(fluids, tagName);
	}

	public static void registerFluid(@Nonnull Set<Fluid> fluids, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		FLUID_CONTAINER.register(fluids, tagName, tagInclude);
	}

	public static void registerBlock(@Nonnull Set<Block> blocks, @Nonnull String tagName) {
		BLOCK_CONTAINER.register(blocks, tagName);
	}

	public static void registerBlock(@Nonnull Set<Block> blocks, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		BLOCK_CONTAINER.register(blocks, tagName, tagInclude);
	}

	public static void replaceItem(@Nonnull Set<ItemKey> itemKeys, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		ITEM_CONTAINER.replace(itemKeys, tagName, tagInclude);
	}

	public static void replaceFluid(@Nonnull Set<Fluid> fluids, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		FLUID_CONTAINER.replace(fluids, tagName, tagInclude);
	}

	public static void replaceBlock(@Nonnull Set<Block> blocks, @Nonnull String tagName,
			@Nonnull Set<String> tagInclude) {
		BLOCK_CONTAINER.replace(blocks, tagName, tagInclude);
	}
}

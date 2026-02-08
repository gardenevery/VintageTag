package com.gardenevery.vintagetag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.bsideup.jabel.Desugar;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

interface TagEntry {
	default boolean isEmpty() {
		return false;
	}

	default boolean isTag() {
		return false;
	}

	@Nullable
	default String getTagName() {
		return null;
	}

	@Nullable
	default String getDisplayTagName() {
		return getTagName() != null ? "#" + getTagName() : null;
	}

	@Nonnull
	static ItemEntry item(@Nullable String name) {
		if (name == null || name.trim().isEmpty()) {
			return ItemEntry.EMPTY;
		}

		if (name.startsWith("#")) {
			var tagName = name.substring(1);
			if (tagName.trim().isEmpty()) {
				return ItemEntry.EMPTY;
			}
			return new ItemEntry.ItemTag(tagName);
		} else {
			var resourceLocation = new ResourceLocation(name);
			var item = ForgeRegistries.ITEMS.getValue(resourceLocation);
			if (item == null) {
				return ItemEntry.EMPTY;
			} else {
				return new ItemEntry.ItemKey(item, 0);
			}
		}
	}

	@Nonnull
	static ItemEntry item(@Nullable String name, int metadata) {
		if (name == null || name.trim().isEmpty() || metadata < 0 || name.startsWith("#")) {
			return ItemEntry.EMPTY;
		}

		var resourceLocation = new ResourceLocation(name);
		var item = ForgeRegistries.ITEMS.getValue(resourceLocation);
		if (item == null) {
			return ItemEntry.EMPTY;
		} else {
			return new ItemEntry.ItemKey(item, item.getHasSubtypes() ? metadata : 0);
		}
	}

	@Nonnull
	static ItemEntry item(@Nullable Item item) {
		if (item == null) {
			return ItemEntry.EMPTY;
		}
		return new ItemEntry.ItemKey(item, 0);
	}

	@Nonnull
	static ItemEntry item(@Nullable Item item, int metadata) {
		if (item == null || metadata < 0) {
			return ItemEntry.EMPTY;
		}
		return new ItemEntry.ItemKey(item, item.getHasSubtypes() ? metadata : 0);
	}

	@Nonnull
	static ItemEntry item(@Nullable ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return ItemEntry.EMPTY;
		}
		return new ItemEntry.ItemKey(stack.getItem(), stack.getHasSubtypes() ? stack.getMetadata() : 0);
	}

	@Nonnull
	static FluidEntry fluid(@Nullable String name) {
		if (name == null || name.trim().isEmpty()) {
			return FluidEntry.EMPTY;
		}

		if (name.startsWith("#")) {
			var tagName = name.substring(1);
			if (tagName.trim().isEmpty()) {
				return FluidEntry.EMPTY;
			}
			return new FluidEntry.FluidTag(tagName);
		} else {
			var fluid = FluidRegistry.getFluid(name);
			if (fluid == null) {
				return FluidEntry.EMPTY;
			} else {
				return new FluidEntry.FluidKey(fluid);
			}
		}
	}

	@Nonnull
	static FluidEntry fluid(@Nullable Fluid fluid) {
		if (fluid == null) {
			return FluidEntry.EMPTY;
		}
		return new FluidEntry.FluidKey(fluid);
	}

	@Nonnull
	static FluidEntry fluid(@Nullable FluidStack stack) {
		if (stack == null || stack.getFluid() == null) {
			return FluidEntry.EMPTY;
		}
		return new FluidEntry.FluidKey(stack.getFluid());
	}

	@Nonnull
	static BlockEntry block(@Nullable String name) {
		if (name == null || name.trim().isEmpty()) {
			return BlockEntry.EMPTY;
		}

		if (name.startsWith("#")) {
			var tagName = name.substring(1);
			if (tagName.trim().isEmpty()) {
				return BlockEntry.EMPTY;
			}
			return new BlockEntry.BlockTag(tagName);
		} else {
			var resourceLocation = new ResourceLocation(name);
			var block = ForgeRegistries.BLOCKS.getValue(resourceLocation);
			if (block == null) {
				return BlockEntry.EMPTY;
			}
			return new BlockEntry.BlockKey(block);
		}
	}

	@Nonnull
	static BlockEntry block(@Nullable Block block) {
		if (block == null) {
			return BlockEntry.EMPTY;
		}
		return new BlockEntry.BlockKey(block);
	}

	@Nonnull
	static BlockEntry block(@Nullable TileEntity blockEntity) {
		if (blockEntity == null) {
			return BlockEntry.EMPTY;
		}
		return new BlockEntry.BlockKey(blockEntity.getBlockType());
	}

	interface ItemEntry extends TagEntry {
		ItemEntry EMPTY = new ItemEntry() {
			@Override
			public boolean isEmpty() {
				return true;
			}
		};

		@Nullable
		default ItemKey toKey() {
			return null;
		}

		@Desugar
		record ItemKey(Item item, int metadata) implements ItemEntry {
			public ItemStack toStack() {
				return new ItemStack(item, 1, metadata);
			}

			@Override
			public ItemKey toKey() {
				return this;
			}
		}

		@Desugar
		record ItemTag(String tagName) implements ItemEntry {
			@Override
			public boolean isTag() {
				return true;
			}

			@Override
			public String getTagName() {
				return tagName;
			}
		}
	}

	interface FluidEntry extends TagEntry {
		FluidEntry EMPTY = new FluidEntry() {
			@Override
			public boolean isEmpty() {
				return true;
			}
		};

		@Nullable
		default FluidKey toKey() {
			return null;
		}

		@Desugar
		record FluidKey(Fluid fluid) implements FluidEntry {
			public FluidStack toStack() {
				return new FluidStack(fluid, 1000);
			}

			@Override
			public FluidKey toKey() {
				return this;
			}
		}

		@Desugar
		record FluidTag(String tagName) implements FluidEntry {
			@Override
			public boolean isTag() {
				return true;
			}

			@Override
			public String getTagName() {
				return tagName;
			}
		}
	}

	interface BlockEntry extends TagEntry {
		BlockEntry EMPTY = new BlockEntry() {
			@Override
			public boolean isEmpty() {
				return true;
			}
		};

		@Nullable
		default BlockKey toKey() {
			return null;
		}

		@Desugar
		record BlockKey(Block block) implements BlockEntry {
			@Override
			public BlockKey toKey() {
				return this;
			}
		}

		@Desugar
		record BlockTag(String tagName) implements BlockEntry {
			@Override
			public boolean isTag() {
				return true;
			}

			@Override
			public String getTagName() {
				return tagName;
			}
		}
	}
}

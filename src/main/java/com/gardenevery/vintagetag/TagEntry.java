package com.gardenevery.vintagetag;

import java.util.Objects;
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
	default boolean isTag() {
		return false;
	}

	@Nullable
	default String getTagName() {
		return null;
	}

	@Nullable
	default String getRawTagName() {
		return "#" + getTagName();
	}

	@Nullable
	static ItemEntry item(String name) {
		if (name == null || name.trim().isEmpty()) {
			return null;
		}

		if (name.startsWith("#")) {
			return ItemEntry.ItemTag.of(name.substring(1));
		} else {
			return ItemEntry.ItemKey.of(name);
		}
	}

	@Nullable
	static ItemEntry item(String name, int metadata) {
		if (name == null || name.trim().isEmpty() || metadata < 0 || name.startsWith("#")) {
			return null;
		}
		return ItemEntry.ItemKey.of(name, metadata);
	}

	@Nullable
	static FluidEntry fluid(String name) {
		if (name == null || name.trim().isEmpty()) {
			return null;
		}

		if (name.startsWith("#")) {
			return FluidEntry.FluidTag.of(name.substring(1));
		} else {
			return FluidEntry.FluidKey.of(name);
		}
	}

	@Nullable
	static BlockEntry block(String name) {
		if (name == null || name.trim().isEmpty()) {
			return null;
		}

		if (name.startsWith("#")) {
			return BlockEntry.BlockTag.of(name.substring(1));
		} else {
			return BlockEntry.BlockKey.of(name);
		}
	}

	interface ItemEntry extends TagEntry {
		@Desugar
		record ItemKey(Item item, int metadata) implements ItemEntry {
			public ItemKey {
				Objects.requireNonNull(item, "Item cannot be null");
				if (metadata < 0) {
					throw new IllegalArgumentException("Metadata cannot be negative");
				}
			}

			public static ItemKey of(String name) {
				var resourceLocation = new ResourceLocation(name);
				return new ItemKey(ForgeRegistries.ITEMS.getValue(resourceLocation), 0);
			}

			public static ItemKey of(String name, int metadata) {
				var resourceLocation = new ResourceLocation(name);
				return new ItemKey(ForgeRegistries.ITEMS.getValue(resourceLocation), metadata);
			}

			public static ItemKey of(Item item) {
				return new ItemKey(item, 0);
			}

			public static ItemKey of(Item item, int metadata) {
				return new ItemKey(item, metadata);
			}

			public static ItemKey of(ItemStack stack) {
				return new ItemKey(stack.getItem(), stack.getHasSubtypes() ? stack.getMetadata() : 0);
			}

			public ItemStack toStack() {
				return new ItemStack(item, 1, metadata);
			}
		}

		@Desugar
		record ItemTag(String tagName) implements ItemEntry {
			public ItemTag {
				Objects.requireNonNull(tagName, "Tag name cannot be null");
				if (tagName.trim().isEmpty()) {
					throw new IllegalArgumentException("Tag name cannot be empty");
				}
			}

			public static ItemTag of(String tagName) {
				return new ItemTag(tagName);
			}

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
		@Desugar
		record FluidKey(Fluid fluid) implements FluidEntry {
			public FluidKey {
				Objects.requireNonNull(fluid, "Fluid cannot be null");
			}

			public static FluidKey of(String name) {
				return new FluidKey(FluidRegistry.getFluid(name));
			}

			public static FluidKey of(Fluid fluid) {
				return new FluidKey(fluid);
			}

			public static FluidKey of(FluidStack stack) {
				return new FluidKey(stack.getFluid());
			}
		}

		@Desugar
		record FluidTag(String tagName) implements FluidEntry {
			public FluidTag {
				Objects.requireNonNull(tagName, "Tag name cannot be null");
				if (tagName.trim().isEmpty()) {
					throw new IllegalArgumentException("Tag name cannot be empty");
				}
			}

			public static FluidTag of(String tagName) {
				return new FluidTag(tagName);
			}

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
		@Desugar
		record BlockKey(Block block) implements BlockEntry {
			public BlockKey {
				Objects.requireNonNull(block, "Block cannot be null");
			}

			public static BlockKey of(String name) {
				var resourceLocation = new ResourceLocation(name);
				return new BlockKey(ForgeRegistries.BLOCKS.getValue(resourceLocation));
			}

			public static BlockKey of(Block block) {
				return new BlockKey(block);
			}

			public static BlockKey of(TileEntity blockEntity) {
				return new BlockKey(blockEntity.getBlockType());
			}
		}

		@Desugar
		record BlockTag(String tagName) implements BlockEntry {
			public BlockTag {
				Objects.requireNonNull(tagName, "Tag name cannot be null");
				if (tagName.trim().isEmpty()) {
					throw new IllegalArgumentException("Tag name cannot be empty");
				}
			}

			public static BlockTag of(String tagName) {
				return new BlockTag(tagName);
			}

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

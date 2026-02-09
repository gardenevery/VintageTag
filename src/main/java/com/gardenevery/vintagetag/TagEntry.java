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
		return itemInternal(name, 0, true);
	}

	@Nonnull
	static ItemEntry item(@Nullable String name, int metadata) {
		return itemInternal(name, metadata, false);
	}

	@Nonnull
	static ItemEntry item(@Nullable Item item) {
		return item == null ? ItemEntry.EMPTY : new ItemEntry.ItemKey(item, 0);
	}

	@Nonnull
	static ItemEntry item(@Nullable Item item, int metadata) {
		return item == null || metadata < 0
				? ItemEntry.EMPTY
				: new ItemEntry.ItemKey(item, item.getHasSubtypes() ? metadata : 0);
	}

	@Nonnull
	static ItemEntry item(@Nullable ItemStack stack) {
		return (stack == null || stack.isEmpty())
				? ItemEntry.EMPTY
				: new ItemEntry.ItemKey(stack.getItem(), stack.getHasSubtypes() ? stack.getMetadata() : 0);
	}

	static ItemEntry itemInternal(String name, int metadata, boolean useZeroForMeta) {
		if (name == null || name.trim().isEmpty())
			return ItemEntry.EMPTY;
		if (name.startsWith("#")) {
			var tagName = extractTagName(name);
			return tagName == null ? ItemEntry.EMPTY : new ItemEntry.ItemTag(tagName);
		}
		var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
		if (item == null)
			return ItemEntry.EMPTY;
		int meta = useZeroForMeta ? 0 : (item.getHasSubtypes() ? metadata : 0);
		return new ItemEntry.ItemKey(item, meta);
	}

	@Nonnull
	static FluidEntry fluid(@Nullable String name) {
		if (name == null || name.trim().isEmpty())
			return FluidEntry.EMPTY;
		if (name.startsWith("#")) {
			var tagName = extractTagName(name);
			return tagName == null ? FluidEntry.EMPTY : new FluidEntry.FluidTag(tagName);
		}
		var fluid = FluidRegistry.getFluid(name);
		return (fluid == null) ? FluidEntry.EMPTY : new FluidEntry.FluidKey(fluid);
	}

	@Nonnull
	static FluidEntry fluid(@Nullable Fluid fluid) {
		return fluid == null ? FluidEntry.EMPTY : new FluidEntry.FluidKey(fluid);
	}

	@Nonnull
	static FluidEntry fluid(@Nullable FluidStack stack) {
		return (stack == null || stack.getFluid() == null)
				? FluidEntry.EMPTY
				: new FluidEntry.FluidKey(stack.getFluid());
	}

	@Nonnull
	static BlockEntry block(@Nullable String name) {
		if (name == null || name.trim().isEmpty())
			return BlockEntry.EMPTY;
		if (name.startsWith("#")) {
			var tagName = extractTagName(name);
			return tagName == null ? BlockEntry.EMPTY : new BlockEntry.BlockTag(tagName);
		}
		var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
		return block == null ? BlockEntry.EMPTY : new BlockEntry.BlockKey(block);
	}

	@Nonnull
	static BlockEntry block(@Nullable Block block) {
		return block == null ? BlockEntry.EMPTY : new BlockEntry.BlockKey(block);
	}

	@Nonnull
	static BlockEntry block(@Nullable TileEntity blockEntity) {
		return blockEntity == null ? BlockEntry.EMPTY : new BlockEntry.BlockKey(blockEntity.getBlockType());
	}

	@Nullable
	static String extractTagName(@Nonnull String name) {
		var tagName = name.substring(1).trim();
		return tagName.isEmpty() ? null : tagName;
	}

	interface ItemEntry extends TagEntry {
		ItemEntry EMPTY = new ItemEntry() {
			public boolean isEmpty() {
				return true;
			}
		};

		@Nullable
		default ItemKey toKey() {
			return null;
		}

		@Nullable
		default ItemTag toTag() {
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
			@Override
			public ItemTag toTag() {
				return this;
			}
		}
	}

	interface FluidEntry extends TagEntry {
		FluidEntry EMPTY = new FluidEntry() {
			public boolean isEmpty() {
				return true;
			}
		};

		@Nullable
		default FluidKey toKey() {
			return null;
		}

		@Nullable
		default FluidTag toTag() {
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

			@Override
			public FluidTag toTag() {
				return this;
			}
		}
	}

	interface BlockEntry extends TagEntry {
		BlockEntry EMPTY = new BlockEntry() {
			public boolean isEmpty() {
				return true;
			}
		};

		@Nullable
		default BlockKey toKey() {
			return null;
		}

		@Nullable
		default BlockTag toTag() {
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

			@Override
			public BlockTag toTag() {
				return this;
			}
		}
	}
}

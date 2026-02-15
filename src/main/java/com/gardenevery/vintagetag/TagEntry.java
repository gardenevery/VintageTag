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
	enum EntryType {
		KEY, TAG, EMPTY
	}

	default EntryType getType() {
		return EntryType.EMPTY;
	}

	default boolean isEmpty() {
		return getType() == EntryType.EMPTY;
	}

	default boolean isKey() {
		return getType() == EntryType.KEY;
	}

	default boolean isTag() {
		return getType() == EntryType.TAG;
	}

	@Nonnull
	default String getTagName() {
		return "";
	}

	@Nonnull
	default String getDisplayTagName() {
		var tagName = getTagName();
		return !tagName.trim().isEmpty() ? "#" + tagName : "";
	}

	@Nonnull
	static ItemEntry item(@Nullable String name) {
		return itemInternal(name, 0, true);
	}

	@Nonnull
	static ItemEntry item(@Nullable String name, int metadata) {
		if (metadata < 0) {
			return ItemEntry.EMPTY;
		}
		return itemInternal(name, metadata, false);
	}

	@Nonnull
	static ItemEntry item(@Nullable ResourceLocation name) {
		if (name == null) {
			return ItemEntry.EMPTY;
		}

		var item = ForgeRegistries.ITEMS.getValue(name);
		return item == null ? ItemEntry.EMPTY : new ItemEntry.ItemKey(item, 0);
	}

	@Nonnull
	static ItemEntry item(@Nullable ResourceLocation name, int metadata) {
		if (name == null || metadata < 0) {
			return ItemEntry.EMPTY;
		}

		var item = ForgeRegistries.ITEMS.getValue(name);
		return item == null ? ItemEntry.EMPTY : new ItemEntry.ItemKey(item, item.getHasSubtypes() ? metadata : 0);
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

	@Nonnull
	static ItemEntry itemTag(@Nullable String tagName) {
		return tagName == null || tagName.trim().isEmpty() ? ItemEntry.EMPTY : new ItemEntry.ItemTagInclude(tagName);
	}

	@Nonnull
	static FluidEntry fluid(@Nullable String name) {
		if (name == null || name.trim().isEmpty()) {
			return FluidEntry.EMPTY;
		}

		if (name.startsWith("#")) {
			var tagName = extractTagName(name);
			return tagName == null ? FluidEntry.EMPTY : new FluidEntry.FluidTagInclude(tagName);
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
	static FluidEntry fluidTag(@Nullable String tagName) {
		return tagName == null || tagName.trim().isEmpty() ? FluidEntry.EMPTY : new FluidEntry.FluidTagInclude(tagName);
	}

	@Nonnull
	static BlockEntry block(@Nullable String name) {
		if (name == null || name.trim().isEmpty()) {
			return BlockEntry.EMPTY;
		}

		if (name.startsWith("#")) {
			var tagName = extractTagName(name);
			return tagName == null ? BlockEntry.EMPTY : new BlockEntry.BlockTagInclude(tagName);
		}
		var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
		return block == null ? BlockEntry.EMPTY : new BlockEntry.BlockKey(block);
	}

	@Nonnull
	static BlockEntry block(@Nullable ResourceLocation name) {
		if (name == null) {
			return BlockEntry.EMPTY;
		}
		var block = ForgeRegistries.BLOCKS.getValue(name);
		return block == null ? BlockEntry.EMPTY : new BlockEntry.BlockKey(block);
	}

	@Nonnull
	static BlockEntry block(@Nullable Block block) {
		return block == null ? BlockEntry.EMPTY : new BlockEntry.BlockKey(block);
	}

	@Nonnull
	static BlockEntry block(@Nullable TileEntity blockEntity) {
		return (blockEntity == null || blockEntity.isInvalid())
				? BlockEntry.EMPTY
				: new BlockEntry.BlockKey(blockEntity.getBlockType());
	}

	@Nonnull
	static BlockEntry blockTag(@Nullable String tagName) {
		return tagName == null || tagName.trim().isEmpty() ? BlockEntry.EMPTY : new BlockEntry.BlockTagInclude(tagName);
	}

	@Nullable
	static String extractTagName(@Nonnull String name) {
		if (name.length() <= 1) {
			return null;
		}

		var tagName = name.substring(1).trim();
		return tagName.isEmpty() ? null : tagName;
	}

	@Nonnull
	static ItemEntry itemInternal(@Nullable String name, int metadata, boolean useZeroForMeta) {
		if (name == null || name.trim().isEmpty()) {
			return ItemEntry.EMPTY;
		}

		if (name.startsWith("#")) {
			var tagName = extractTagName(name);
			return tagName == null ? ItemEntry.EMPTY : new ItemEntry.ItemTagInclude(tagName);
		}

		var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
		if (item == null) {
			return ItemEntry.EMPTY;
		}

		int meta = useZeroForMeta ? 0 : (item.getHasSubtypes() ? metadata : 0);
		return new ItemEntry.ItemKey(item, meta);
	}

	interface TagKey extends TagEntry {
		@Override
		default EntryType getType() {
			return EntryType.KEY;
		}
	}

	interface TagInclude extends TagEntry {
		@Override
		default EntryType getType() {
			return EntryType.TAG;
		}

		@Nonnull
		@Override
		String getTagName();

		@Nonnull
		@Override
		default String getDisplayTagName() {
			return "#" + getTagName();
		}
	}

	interface ItemEntry extends TagEntry {
		ItemEntry EMPTY = new ItemEntry() {
		};

		@Desugar
		record ItemKey(Item item, int metadata) implements TagKey, ItemEntry {
			@Nonnull
			public ItemStack getStack() {
				return new ItemStack(item, 1, metadata);
			}
		}

		@Desugar
		record ItemTagInclude(String tagName) implements TagInclude, ItemEntry {
			@Nonnull
			@Override
			public String getTagName() {
				return tagName;
			}
		}
	}

	interface FluidEntry extends TagEntry {
		FluidEntry EMPTY = new FluidEntry() {
		};

		@Desugar
		record FluidKey(Fluid fluid) implements TagKey, FluidEntry {
			@Nonnull
			public FluidStack getStack() {
				return new FluidStack(fluid, 1000);
			}
		}

		@Desugar
		record FluidTagInclude(String tagName) implements TagInclude, FluidEntry {
			@Nonnull
			@Override
			public String getTagName() {
				return tagName;
			}
		}
	}

	interface BlockEntry extends TagEntry {
		BlockEntry EMPTY = new BlockEntry() {
		};

		@Desugar
		record BlockKey(Block block) implements TagKey, BlockEntry {
		}

		@Desugar
		record BlockTagInclude(String tagName) implements TagInclude, BlockEntry {
			@Nonnull
			@Override
			public String getTagName() {
				return tagName;
			}
		}
	}
}

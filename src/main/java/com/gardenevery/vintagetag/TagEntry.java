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

	enum TagEntryType {
		ITEM_KEY, ITEM_TAG_INCLUDE, FLUID_KEY, FLUID_TAG_INCLUDE, BLOCK_KEY, BLOCK_TAG_INCLUDE, EMPTY
	}

	boolean isEmpty();

	default TagEntryType getType() {
		return TagEntryType.EMPTY;
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
		if (metadata < 0) {
			return ItemEntry.EMPTY;
		}
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
	static BlockEntry block(@Nullable Block block) {
		return block == null ? BlockEntry.EMPTY : new BlockEntry.BlockKey(block);
	}

	static BlockEntry block(@Nullable TileEntity blockEntity) {
		return (blockEntity == null || blockEntity.isInvalid())
				? BlockEntry.EMPTY
				: new BlockEntry.BlockKey(blockEntity.getBlockType());
	}

	@Nullable
	static String extractTagName(@Nonnull String name) {
		var tagName = name.substring(1).trim();
		return tagName.isEmpty() ? null : tagName;
	}

	interface TagKey<V, E> extends TagEntry {
		@Override
		default boolean isEmpty() {
			return false;
		}

		@Nonnull
		V getValue();

		@Nonnull
		E getElement();
	}

	interface TagInclude extends TagEntry {
		@Override
		default boolean isEmpty() {
			return false;
		}

		@Override
		default boolean isTag() {
			return true;
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
		ItemEntry EMPTY = () -> true;

		@Nullable
		default ItemKey toKey() {
			return null;
		}

		@Nullable
		default ItemTagInclude toTag() {
			return null;
		}

		@Desugar
		record ItemKey(Item item, int metadata) implements TagKey<Item, ItemStack>, ItemEntry {
			@Nonnull
			@Override
			public ItemKey toKey() {
				return this;
			}

			@Override
			public TagEntryType getType() {
				return TagEntryType.ITEM_KEY;
			}

			@Nonnull
			@Override
			public Item getValue() {
				return item;
			}

			@Nonnull
			@Override
			public ItemStack getElement() {
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

			@Override
			public TagEntryType getType() {
				return TagEntryType.ITEM_TAG_INCLUDE;
			}

			@Nonnull
			@Override
			public ItemTagInclude toTag() {
				return this;
			}
		}
	}

	interface FluidEntry extends TagEntry {
		FluidEntry EMPTY = () -> true;

		@Nullable
		default FluidKey toKey() {
			return null;
		}

		@Nullable
		default FluidTagInclude toTag() {
			return null;
		}

		@Desugar
		record FluidKey(Fluid fluid) implements TagKey<Fluid, FluidStack>, FluidEntry {
			@Nonnull
			@Override
			public FluidKey toKey() {
				return this;
			}

			@Override
			public TagEntryType getType() {
				return TagEntryType.FLUID_KEY;
			}

			@Nonnull
			@Override
			public Fluid getValue() {
				return fluid;
			}

			@Nonnull
			@Override
			public FluidStack getElement() {
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

			@Override
			public TagEntryType getType() {
				return TagEntryType.FLUID_TAG_INCLUDE;
			}

			@Nonnull
			@Override
			public FluidTagInclude toTag() {
				return this;
			}
		}
	}

	interface BlockEntry extends TagEntry {
		BlockEntry EMPTY = () -> true;

		@Nullable
		default BlockKey toKey() {
			return null;
		}

		@Nullable
		default BlockTagInclude toTag() {
			return null;
		}

		@Desugar
		record BlockKey(Block block) implements TagKey<Block, Block>, BlockEntry {
			@Nonnull
			@Override
			public BlockKey toKey() {
				return this;
			}

			@Override
			public TagEntryType getType() {
				return TagEntryType.BLOCK_KEY;
			}

			@Nonnull
			@Override
			public Block getValue() {
				return block;
			}

			@Nonnull
			@Override
			public Block getElement() {
				return block;
			}
		}

		@Desugar
		record BlockTagInclude(String tagName) implements TagInclude, BlockEntry {
			@Nonnull
			@Override
			public String getTagName() {
				return tagName;
			}

			@Override
			public TagEntryType getType() {
				return TagEntryType.BLOCK_TAG_INCLUDE;
			}

			@Nonnull
			@Override
			public BlockTagInclude toTag() {
				return this;
			}
		}
	}
}

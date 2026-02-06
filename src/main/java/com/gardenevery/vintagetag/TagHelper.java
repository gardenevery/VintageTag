package com.gardenevery.vintagetag;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class TagHelper {

	private TagHelper() {
	}

	/**
	 * TagHelper.item()
	 */
	@Nonnull
	public static ItemTagHelper item() {
		return ItemTagHelper.INSTANCE;
	}

	/**
	 * TagHelper.fluid()
	 */
	@Nonnull
	public static FluidTagHelper fluid() {
		return FluidTagHelper.INSTANCE;
	}

	/**
	 * TagHelper.block()
	 */
	@Nonnull
	public static BlockTagHelper block() {
		return BlockTagHelper.INSTANCE;
	}

	/**
	 * Get the total number of tags
	 *
	 * @return The total number of tags
	 */
	public static int tagCount() {
		return TagManager.item().getTagCount() + TagManager.fluid().getTagCount() + TagManager.block().getTagCount();
	}

	/**
	 * Get the total number of tag associations
	 *
	 * @return The total number of tag associations
	 */
	public static int associationCount() {
		return TagManager.item().getAssociationCount() + TagManager.fluid().getAssociationCount()
				+ TagManager.block().getAssociationCount();
	}

	/**
	 * Get the total number of tag keys
	 *
	 * @return The total number of tag keys
	 */
	public static int keyCount() {
		return TagManager.item().getKeyCount() + TagManager.fluid().getKeyCount() + TagManager.block().getKeyCount();
	}

	/**
	 * Check if a tag exists
	 *
	 * @param tagName
	 *            The tag name to check, can be null
	 * @return true if the tag exists, false otherwise
	 */
	public boolean exists(@Nullable String tagName) {
		if (tagInvalid(tagName)) {
			return false;
		}
		return TagManager.item().exists(tagName) || TagManager.fluid().exists(tagName)
				|| TagManager.block().exists(tagName);
	}

	private static boolean tagInvalid(@Nullable String tagName) {
		return tagName == null || tagName.isEmpty();
	}

	private static boolean tagInvalid(@Nullable String... tagNames) {
		return tagNames == null || tagNames.length == 0;
	}

	/**
	 * Helper class for item tag operations
	 */
	public static final class ItemTagHelper {

		private static final ItemTagHelper INSTANCE = new ItemTagHelper();

		private ItemTagHelper() {
		}

		/**
		 * Get all tags associated with the specified Item
		 *
		 * @param item
		 *            The Item to query, can be null
		 * @return An unmodifiable set of tag names, empty if item is null
		 */
		@Nonnull
		public Set<String> tags(@Nullable Item item) {
			return (item == null)
					? Collections.emptySet()
					: TagManager.item().getTags(ItemKey.of(item));
		}

		/**
		 * Get all tags associated with the specified ItemStack
		 *
		 * @param stack
		 *            The ItemStack to query, can be null
		 * @return An unmodifiable set of tag names, empty if stack is null or empty
		 */
		@Nonnull
		public Set<String> tags(@Nullable ItemStack stack) {
			return (stack == null || stack.isEmpty())
					? Collections.emptySet()
					: TagManager.item().getTags(ItemKey.of(stack));
		}

		/**
		 * Get all tags associated with the specified Item as a List
		 *
		 * @param item
		 *            The Item to query, can be null
		 * @return An unmodifiable list of tag names, empty if item is null
		 */
		@Nonnull
		public List<String> tagsList(@Nullable Item item) {
			return (item == null)
					? Collections.emptyList()
					: TagManager.item().getTagsList(ItemKey.of(item));
		}

		/**
		 * Get all tags associated with the specified ItemStack as a List
		 *
		 * @param stack
		 *            The ItemStack to query, can be null
		 * @return An unmodifiable list of tag names, empty if stack is null or empty
		 */
		@Nonnull
		public List<String> tagsList(@Nullable ItemStack stack) {
			return (stack == null || stack.isEmpty())
					? Collections.emptyList()
					: TagManager.item().getTagsList(ItemKey.of(stack));
		}

		/**
		 * Get all tags defined for items
		 *
		 * @return An unmodifiable set of all item tag names
		 */
		@Nonnull
		public Set<String> allTags() {
			return TagManager.item().getAllTags();
		}

		/**
		 * Get all tags defined for items as a List
		 *
		 * @return An unmodifiable list of all item tag names
		 */
		@Nonnull
		public List<String> allTagsList() {
			return TagManager.item().getAllTagsList();
		}

		/**
		 * Get all ItemStacks associated with the specified tag name
		 *
		 * @param tagName
		 *            The tag name to query, can be null
		 * @return An unmodifiable set of ItemStacks, empty if tagName is null or empty
		 */
		@Nonnull
		public Set<ItemStack> keys(@Nullable String tagName) {
			if (tagInvalid(tagName)) {
				return Collections.emptySet();
			}

			var keys = TagManager.item().getKeys(tagName);
			Set<ItemStack> stacks = new ObjectOpenHashSet<>();

			for (var key : keys) {
				stacks.add(key.toStack());
			}
			return stacks;
		}

		/**
		 * Get all ItemStacks associated with the specified tag name as a List
		 *
		 * @param tagName
		 *            The tag name to query, can be null
		 * @return An unmodifiable list of ItemStacks, empty if tagName is null or empty
		 */
		@Nonnull
		public List<ItemStack> keysList(@Nullable String tagName) {
			if (tagInvalid(tagName)) {
				return Collections.emptyList();
			}

			var keys = TagManager.item().getKeysList(tagName);
			if (keys.isEmpty()) {
				return Collections.emptyList();
			}

			List<ItemStack> stacks = new ObjectArrayList<>(keys.size());
			for (var key : keys) {
				stacks.add(key.toStack());
			}
			return Collections.unmodifiableList(stacks);
		}

		/**
		 * Get all ItemStacks that have at least one tag
		 *
		 * @return An unmodifiable set of all tagged ItemStacks
		 */
		@Nonnull
		public Set<ItemStack> allKeys() {
			var keys = TagManager.item().getAllKeys();
			Set<ItemStack> stacks = new ObjectOpenHashSet<>();

			for (var key : keys) {
				stacks.add(key.toStack());
			}
			return stacks;
		}

		/**
		 * Get all ItemStacks that have at least one tag as a List
		 *
		 * @return An unmodifiable list of all tagged ItemStacks
		 */
		@Nonnull
		public List<ItemStack> allKeysList() {
			var keys = TagManager.item().getAllKeysList();
			if (keys.isEmpty()) {
				return Collections.emptyList();
			}

			List<ItemStack> stacks = new ObjectArrayList<>(keys.size());
			for (var key : keys) {
				stacks.add(key.toStack());
			}
			return Collections.unmodifiableList(stacks);
		}

		/**
		 * Get all item tag entries with their associated ItemStacks
		 *
		 * @return An unmodifiable map of tag name to immutable set of ItemStacks
		 */
		@Nonnull
		public Map<String, Set<ItemStack>> allEntries() {
			var keyMap = TagManager.item().getAllEntries();

			return keyMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
					entry -> entry.getValue().stream().map(ItemKey::toStack).collect(Collectors.toSet())));
		}

		/**
		 * Check if the specified Item has the given tag
		 *
		 * @param item
		 *            The Item to check, can be null
		 * @param tagName
		 *            The tag name to check for, can be null
		 * @return true if item is not null, tagName is valid, and item has the tag
		 */
		public boolean hasTag(@Nullable Item item, @Nullable String tagName) {
			if (item == null || tagInvalid(tagName)) {
				return false;
			}
			var key = ItemKey.of(item);
			return TagManager.item().hasTag(key, tagName);
		}

		/**
		 * Check if the specified ItemStack has the given tag
		 *
		 * @param stack
		 *            The ItemStack to check, can be null
		 * @param tagName
		 *            The tag name to check for, can be null
		 * @return true if stack is not null/empty, tagName is valid, and stack has the
		 *         tag
		 */
		public boolean hasTag(@Nullable ItemStack stack, @Nullable String tagName) {
			if (stack == null || stack.isEmpty() || tagInvalid(tagName)) {
				return false;
			}
			var key = ItemKey.of(stack);
			return TagManager.item().hasTag(key, tagName);
		}

		/**
		 * Check if the specified Item has any of the given tags
		 *
		 * @param item
		 *            The Item to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if item is not null, tagNames are valid, and item has any of
		 *         the tags
		 */
		public boolean hasAnyTag(@Nullable Item item, @Nullable String... tagNames) {
			if (item == null || tagInvalid(tagNames)) {
				return false;
			}
			var key = ItemKey.of(item);
			return TagManager.item().hasAnyTag(key, tagNames);
		}

		/**
		 * Check if the specified ItemStack has any of the given tags
		 *
		 * @param stack
		 *            The ItemStack to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if stack is not null/empty, tagNames are valid, and stack has
		 *         any of the tags
		 */
		public boolean hasAnyTag(@Nullable ItemStack stack, @Nullable String... tagNames) {
			if (stack == null || stack.isEmpty() || tagInvalid(tagNames)) {
				return false;
			}
			var key = ItemKey.of(stack);
			return TagManager.item().hasAnyTag(key, tagNames);
		}

		/**
		 * Check if the specified Item has all the given tags
		 *
		 * @param item
		 *            The Item to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if item is not null, tagNames are valid, and item has all the
		 *         tags
		 */
		public boolean hasAllTags(@Nullable Item item, @Nullable String... tagNames) {
			if (item == null || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.item().hasAllTags(ItemKey.of(item), tagNames);
		}

		/**
		 * Check if the specified ItemStack has all the given tags
		 *
		 * @param stack
		 *            The ItemStack to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if stack is not null/empty, tagNames are valid, and stack has
		 *         all the tags
		 */
		public boolean hasAllTags(@Nullable ItemStack stack, @Nullable String... tagNames) {
			if (stack == null || stack.isEmpty() || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.item().hasAllTags(ItemKey.of(stack), tagNames);
		}

		/**
		 * Check if the specified Item is tagged
		 *
		 * @param item
		 *            The Item to check, can be null
		 * @return true if item is not null and is tagged
		 */
		public boolean isTagged(@Nullable Item item) {
			if (item == null) {
				return false;
			}
			return TagManager.item().isTagged(ItemKey.of(item));
		}

		/**
		 * Check if the specified ItemStack is tagged
		 *
		 * @param stack
		 *            The ItemStack to check, can be null
		 * @return true if stack is not null/empty and is tagged
		 */
		public boolean isTagged(@Nullable ItemStack stack) {
			if (stack == null || stack.isEmpty()) {
				return false;
			}
			return TagManager.item().isTagged(ItemKey.of(stack));
		}

		/**
		 * Get the total number of tags defined for items
		 *
		 * @return The count of unique item tags
		 */
		public int tagCount() {
			return TagManager.item().getTagCount();
		}

		/**
		 * Get the total number of items that have at least one tag
		 *
		 * @return The count of unique tagged items
		 */
		public int keyCount() {
			return TagManager.item().getKeyCount();
		}

		/**
		 * Get the total number of item-tag associations
		 *
		 * @return The count of all item-tag pairs
		 */
		public int associationCount() {
			return TagManager.item().getAssociationCount();
		}

		/**
		 * Check if the specified tag name exists for items
		 *
		 * @param tagName
		 *            The tag name to check, can be null
		 * @return true if tagName is valid and exists in item tags
		 */
		public boolean exists(@Nullable String tagName) {
			if (tagInvalid(tagName)) {
				return false;
			}
			return TagManager.item().exists(tagName);
		}
	}

	/**
	 * Helper class for fluid tag operations
	 */
	public static final class FluidTagHelper {

		private static final FluidTagHelper INSTANCE = new FluidTagHelper();

		private FluidTagHelper() {
		}

		/**
		 * Get all tags associated with the specified Fluid
		 *
		 * @param fluid
		 *            The Fluid to query, can be null
		 * @return An unmodifiable set of tag names, empty if fluid is null
		 */
		@Nonnull
		public Set<String> tags(@Nullable Fluid fluid) {
			return (fluid == null) ? Collections.emptySet() : TagManager.fluid().getTags(fluid);
		}

		/**
		 * Get all tags associated with the specified FluidStack
		 *
		 * @param stack
		 *            The FluidStack to query, can be null
		 * @return An unmodifiable set of tag names, empty if stack is null or has no
		 *         fluid
		 */
		@Nonnull
		public Set<String> tags(@Nullable FluidStack stack) {
			return (stack == null || stack.getFluid() == null)
					? Collections.emptySet()
					: TagManager.fluid().getTags(stack.getFluid());
		}

		/**
		 * Get all tags associated with the specified Fluid as a List
		 *
		 * @param fluid
		 *            The Fluid to query, can be null
		 * @return An unmodifiable list of tag names, empty if fluid is null
		 */
		@Nonnull
		public List<String> tagsList(@Nullable Fluid fluid) {
			return (fluid == null) ? Collections.emptyList() : TagManager.fluid().getTagsList(fluid);
		}

		/**
		 * Get all tags associated with the specified FluidStack as a List
		 *
		 * @param stack
		 *            The FluidStack to query, can be null
		 * @return An unmodifiable list of tag names, empty if stack is null or has no
		 *         fluid
		 */
		@Nonnull
		public List<String> tagsList(@Nullable FluidStack stack) {
			return (stack == null || stack.getFluid() == null)
					? Collections.emptyList()
					: TagManager.fluid().getTagsList(stack.getFluid());
		}

		/**
		 * Get all tags defined for fluids
		 *
		 * @return An unmodifiable set of all fluid tag names
		 */
		@Nonnull
		public Set<String> allTags() {
			return TagManager.fluid().getAllTags();
		}

		/**
		 * Get all tags defined for fluids as a List
		 *
		 * @return An unmodifiable list of all fluid tag names
		 */
		@Nonnull
		public List<String> allTagsList() {
			return TagManager.fluid().getAllTagsList();
		}

		/**
		 * Get all Fluids that have the given tag
		 *
		 * @param tagName
		 *            The tag name to check for, can be null
		 * @return An unmodifiable set of Fluids, empty if tagName is null or invalid
		 */
		@Nonnull
		public Set<Fluid> keys(@Nullable String tagName) {
			if (tagInvalid(tagName)) {
				return Collections.emptySet();
			}
			return TagManager.fluid().getKeys(tagName);
		}

		/**
		 * Get all Fluids that have the given tag as a List
		 *
		 * @param tagName
		 *            The tag name to check for, can be null
		 * @return An unmodifiable list of Fluids, empty if tagName is null or invalid
		 */
		@Nonnull
		public List<Fluid> keysList(@Nullable String tagName) {
			if (tagInvalid(tagName)) {
				return Collections.emptyList();
			}
			return TagManager.fluid().getKeysList(tagName);
		}

		/**
		 * Get all Fluids that have at least one tag
		 *
		 * @return An unmodifiable set of all tagged Fluids
		 */
		@Nonnull
		public Set<Fluid> allKeys() {
			return TagManager.fluid().getAllKeys();
		}

		/**
		 * Get all Fluids that have at least one tag as a List
		 *
		 * @return An unmodifiable list of all tagged Fluids
		 */
		@Nonnull
		public List<Fluid> allKeysList() {
			return TagManager.fluid().getAllKeysList();
		}

		/**
		 * Get all fluid tag entries with their associated Fluids
		 *
		 * @return An unmodifiable map of tag name to immutable set of Fluids
		 */
		@Nonnull
		public Map<String, Set<Fluid>> allEntries() {
			return Collections.unmodifiableMap(TagManager.fluid().getAllEntries());
		}

		/**
		 * Check if the specified Fluid has the given tag
		 *
		 * @param fluid
		 *            The Fluid to check, can be null
		 * @param tagName
		 *            The tag name to check for, can be null
		 * @return true if fluid is not null, tagName is valid, and fluid has the tag
		 */
		public boolean hasTag(@Nullable Fluid fluid, @Nullable String tagName) {
			if (fluid == null || tagInvalid(tagName)) {
				return false;
			}
			return TagManager.fluid().hasTag(fluid, tagName);
		}

		/**
		 * Check if the specified FluidStack has the given tag
		 *
		 * @param stack
		 *            The FluidStack to check, can be null
		 * @param tagName
		 *            The tag name to check for, can be null
		 * @return true if stack is not null/has fluid, tagName is valid, and fluid has
		 *         the tag
		 */
		public boolean hasTag(@Nullable FluidStack stack, @Nullable String tagName) {
			if (stack == null || stack.getFluid() == null || tagInvalid(tagName)) {
				return false;
			}
			return TagManager.fluid().hasTag(stack.getFluid(), tagName);
		}

		/**
		 * Check if the specified Fluid has any of the given tags
		 *
		 * @param fluid
		 *            The Fluid to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if fluid is not null, tagNames are valid, and fluid has any of
		 *         the tags
		 */
		public boolean hasAnyTag(@Nullable Fluid fluid, @Nullable String... tagNames) {
			if (fluid == null || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.fluid().hasAnyTag(fluid, tagNames);
		}

		/**
		 * Check if the specified FluidStack has any of the given tags
		 *
		 * @param stack
		 *            The FluidStack to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if stack is not null/has fluid, tagNames are valid, and fluid
		 *         has any of the tags
		 */
		public boolean hasAnyTag(@Nullable FluidStack stack, @Nullable String... tagNames) {
			if (stack == null || stack.getFluid() == null || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.fluid().hasAnyTag(stack.getFluid(), tagNames);
		}

		/**
		 * Check if the specified Fluid has all the given tags
		 *
		 * @param fluid
		 *            The Fluid to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if fluid is not null, tagNames are valid, and fluid has all the
		 *         tags
		 */
		public boolean hasAllTags(@Nullable Fluid fluid, @Nullable String... tagNames) {
			if (fluid == null || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.fluid().hasAllTags(fluid, tagNames);
		}

		/**
		 * Check if the specified FluidStack has all the given tags
		 *
		 * @param stack
		 *            The FluidStack to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if stack is not null/has fluid, tagNames are valid, and fluid
		 *         has all the tags
		 */
		public boolean hasAllTags(@Nullable FluidStack stack, @Nullable String... tagNames) {
			if (stack == null || stack.getFluid() == null || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.fluid().hasAllTags(stack.getFluid(), tagNames);
		}

		/**
		 * Check if the specified Fluid is tagged
		 *
		 * @param fluid
		 *            The Fluid to check, can be null
		 * @return true if fluid is not null, and fluid is tagged
		 */
		public boolean isTagged(@Nullable Fluid fluid) {
			if (fluid == null) {
				return false;
			}
			return TagManager.fluid().isTagged(fluid);
		}

		/**
		 * Check if the specified FluidStack is tagged
		 *
		 * @param stack
		 *            The FluidStack to check, can be null
		 * @return true if stack is not null/has fluid, and fluid is tagged
		 */
		public boolean isTagged(@Nullable FluidStack stack) {
			if (stack == null || stack.getFluid() == null) {
				return false;
			}
			return TagManager.fluid().isTagged(stack.getFluid());
		}

		/**
		 * Get the total number of tags defined for fluids
		 *
		 * @return The count of unique fluid tags
		 */
		public int tagCount() {
			return TagManager.fluid().getTagCount();
		}

		/**
		 * Get the total number of fluids that have at least one tag
		 *
		 * @return The count of unique tagged fluids
		 */
		public int keyCount() {
			return TagManager.fluid().getKeyCount();
		}

		/**
		 * Get the total number of fluid-tag associations
		 *
		 * @return The count of all fluid-tag pairs
		 */
		public int associationCount() {
			return TagManager.fluid().getAssociationCount();
		}

		/**
		 * Check if the specified tag name exists for fluids
		 *
		 * @param tagName
		 *            The tag name to check, can be null
		 * @return true if tagName is valid and exists in fluid tags
		 */
		public boolean exists(@Nullable String tagName) {
			if (tagInvalid(tagName)) {
				return false;
			}
			return TagManager.fluid().exists(tagName);
		}
	}

	/**
	 * Helper class for block tag operations
	 */
	public static final class BlockTagHelper {

		private static final BlockTagHelper INSTANCE = new BlockTagHelper();

		private BlockTagHelper() {
		}

		/**
		 * Get all tags associated with the specified Block
		 *
		 * @param block
		 *            The Block to query, can be null
		 * @return An unmodifiable set of tag names, empty if block is null
		 */
		@Nonnull
		public Set<String> tags(@Nullable Block block) {
			return block == null ? Collections.emptySet() : TagManager.block().getTags(block);
		}

		/**
		 * Get all tags associated with the specified TileEntity
		 *
		 * @param blockEntity
		 *            The TileEntity to query, can be null
		 * @return An unmodifiable set of tag names, empty if blockEntity is null
		 */
		@Nonnull
		public Set<String> tags(@Nullable TileEntity blockEntity) {
			return blockEntity == null
					? Collections.emptySet()
					: TagManager.block().getTags(blockEntity.getBlockType());
		}

		/**
		 * Get all tags associated with the specified Block as a List
		 *
		 * @param block
		 *            The Block to query, can be null
		 * @return An unmodifiable list of tag names, empty if block is null
		 */
		@Nonnull
		public List<String> tagsList(@Nullable Block block) {
			return block == null ? Collections.emptyList() : TagManager.block().getTagsList(block);
		}

		/**
		 * Get all tags associated with the specified TileEntity as a List
		 *
		 * @param blockEntity
		 *            The TileEntity to query, can be null
		 * @return An unmodifiable list of tag names, empty if blockEntity is null
		 */
		@Nonnull
		public List<String> tagsList(@Nullable TileEntity blockEntity) {
			return blockEntity == null
					? Collections.emptyList()
					: TagManager.block().getTagsList(blockEntity.getBlockType());
		}

		/**
		 * Get all tags defined for blocks
		 *
		 * @return An unmodifiable set of all block tag names
		 */
		@Nonnull
		public Set<String> allTags() {
			return TagManager.block().getAllTags();
		}

		/**
		 * Get all tags defined for blocks as a List
		 *
		 * @return An unmodifiable list of all block tag names
		 */
		@Nonnull
		public List<String> allTagsList() {
			return TagManager.block().getAllTagsList();
		}

		/**
		 * Get all Blocks associated with the specified tag name
		 *
		 * @param tagName
		 *            The tag name to query, can be null
		 * @return An unmodifiable set of Blocks, empty if tagName is null or empty
		 */
		@Nonnull
		public Set<Block> keys(@Nullable String tagName) {
			if (tagInvalid(tagName)) {
				return Collections.emptySet();
			}
			return TagManager.block().getKeys(tagName);
		}

		/**
		 * Get all Blocks associated with the specified tag name as a List
		 *
		 * @param tagName
		 *            The tag name to query, can be null
		 * @return An unmodifiable list of Blocks, empty if tagName is null or empty
		 */
		@Nonnull
		public List<Block> keysList(@Nullable String tagName) {
			if (tagInvalid(tagName)) {
				return Collections.emptyList();
			}
			return TagManager.block().getKeysList(tagName);
		}

		/**
		 * Get all Blocks that have at least one tag
		 *
		 * @return An unmodifiable set of all tagged Blocks
		 */
		@Nonnull
		public Set<Block> allKeys() {
			return TagManager.block().getAllKeys();
		}

		/**
		 * Get all Blocks that have at least one tag as a List
		 *
		 * @return An unmodifiable list of all tagged Blocks
		 */
		@Nonnull
		public List<Block> allKeysList() {
			return TagManager.block().getAllKeysList();
		}

		/**
		 * Get all block tag entries with their associated Blocks
		 *
		 * @return An unmodifiable map of tag name to immutable set of Blocks
		 */
		@Nonnull
		public Map<String, Set<Block>> allEntries() {
			return Collections.unmodifiableMap(TagManager.block().getAllEntries());
		}

		/**
		 * Check if the specified Block has the given tag
		 *
		 * @param block
		 *            The Block to check, can be null
		 * @param tagName
		 *            The tag name to check for, can be null
		 * @return true if block is not null, tagName is valid, and block has the tag
		 */
		public boolean hasTag(@Nullable Block block, @Nullable String tagName) {
			if (block == null || tagInvalid(tagName)) {
				return false;
			}
			return TagManager.block().hasTag(block, tagName);
		}

		/**
		 * Check if the specified TileEntity's block has the given tag
		 *
		 * @param blockEntity
		 *            The TileEntity to check, can be null
		 * @param tagName
		 *            The tag name to check for, can be null
		 * @return true if blockEntity is not null, tagName is valid, and block has the
		 *         tag
		 */
		public boolean hasTag(@Nullable TileEntity blockEntity, @Nullable String tagName) {
			if (blockEntity == null || tagInvalid(tagName)) {
				return false;
			}
			return TagManager.block().hasTag(blockEntity.getBlockType(), tagName);
		}

		/**
		 * Check if the specified Block has any of the given tags
		 *
		 * @param block
		 *            The Block to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if block is not null, tagNames are valid, and block has any of
		 *         the tags
		 */
		public boolean hasAnyTag(@Nullable Block block, @Nullable String... tagNames) {
			if (block == null || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.block().hasAnyTag(block, tagNames);
		}

		/**
		 * Check if the specified TileEntity's block has any of the given tags
		 *
		 * @param blockEntity
		 *            The TileEntity to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if blockEntity is not null, tagNames are valid, and block has
		 *         any of the tags
		 */
		public boolean hasAnyTag(@Nullable TileEntity blockEntity, @Nullable String... tagNames) {
			if (blockEntity == null || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.block().hasAnyTag(blockEntity.getBlockType(), tagNames);
		}

		/**
		 * Check if the specified Block has all the given tags
		 *
		 * @param block
		 *            The Block to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if block is not null, tagNames are valid, and block has all the
		 *         tags
		 */
		public boolean hasAllTags(@Nullable Block block, @Nullable String... tagNames) {
			if (block == null || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.block().hasAllTags(block, tagNames);
		}

		/**
		 * Check if the specified TileEntity's block has all the given tags
		 *
		 * @param blockEntity
		 *            The TileEntity to check, can be null
		 * @param tagNames
		 *            The tag names to check for, can be null or empty
		 * @return true if blockEntity is not null, tagNames are valid, and block has
		 *         all the tags
		 */
		public boolean hasAllTags(@Nullable TileEntity blockEntity, @Nullable String... tagNames) {
			if (blockEntity == null || tagInvalid(tagNames)) {
				return false;
			}
			return TagManager.block().hasAllTags(blockEntity.getBlockType(), tagNames);
		}

		/**
		 * Check if the specified Block is tagged
		 *
		 * @param block
		 *            The Block to check, can be null
		 * @return true if block is not null and is tagged
		 */
		public boolean isTagged(@Nullable Block block) {
			if (block == null) {
				return false;
			}
			return TagManager.block().isTagged(block);
		}

		/**
		 * Check if the specified TileEntity is tagged
		 *
		 * @param blockEntity
		 *            The TileEntity to check, can be null
		 * @return true if blockEntity is not null and is tagged
		 */
		public boolean isTagged(@Nullable TileEntity blockEntity) {
			if (blockEntity == null) {
				return false;
			}
			return TagManager.block().isTagged(blockEntity.getBlockType());
		}

		/**
		 * Get the total number of tags defined for blocks
		 *
		 * @return The count of unique block tags
		 */
		public int tagCount() {
			return TagManager.block().getTagCount();
		}

		/**
		 * Get the total number of blocks that have at least one tag
		 *
		 * @return The count of unique tagged blocks
		 */
		public int keyCount() {
			return TagManager.block().getKeyCount();
		}

		/**
		 * Get the total number of block-tag associations
		 *
		 * @return The count of all block-tag pairs
		 */
		public int associationCount() {
			return TagManager.block().getAssociationCount();
		}

		/**
		 * Check if the specified tag name exists for blocks
		 *
		 * @param tagName
		 *            The tag name to check, can be null
		 * @return true if tagName is valid and exists in block tags
		 */
		public boolean exists(@Nullable String tagName) {
			if (tagInvalid(tagName)) {
				return false;
			}
			return TagManager.block().exists(tagName);
		}
	}
}

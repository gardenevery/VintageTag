package com.gardenevery.vintagetag;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import com.gardenevery.vintagetag.TagEntry.ItemEntry;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

final class OreDictSync {
	public static void sync() {
		syncAllOreDictionaryTags();
	}

	private static void syncAllOreDictionaryTags() {
		var oreNames = OreDictionary.getOreNames();
		int totalTags = 0;
		int totalItems = 0;

		for (var oreName : oreNames) {
			if (oreName == null || oreName.isEmpty()) {
				continue;
			}

			int itemsInTag = syncSingleOreDictionaryTag(oreName);
			if (itemsInTag > 0) {
				totalTags++;
				totalItems += itemsInTag;
			}
		}

		TagLog.info("OreDictionary sync completed, {} tags, {} items", totalTags, totalItems);
	}

	private static int syncSingleOreDictionaryTag(String oreName) {
		var ores = OreDictionary.getOres(oreName, false);
		ObjectOpenHashSet<ItemEntry> entries = new ObjectOpenHashSet<>();

		for (var stack : ores) {
			if (stack == null || stack.isEmpty()) {
				continue;
			}

			try {
				processItemStack(stack, entries);
			} catch (Exception e) {
				//
			}
		}

		if (!entries.isEmpty()) {
			TagManager.registerItem(entries, oreName);
			return entries.size();
		}

		return 0;
	}

	private static void processItemStack(ItemStack stack, ObjectOpenHashSet<ItemEntry> entries) {
		if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE && stack.getItem().getHasSubtypes()) {
			var wildcardEntries = syncWildcardEntry(stack.getItem());
			entries.addAll(wildcardEntries);
		} else {
			var entry = TagEntry.item(stack);
			entries.add(entry);
		}
	}

	private static ObjectOpenHashSet<ItemEntry> syncWildcardEntry(Item item) {
		NonNullList<ItemStack> stacks = NonNullList.create();
		item.getSubItems(CreativeTabs.SEARCH, stacks);

		ObjectOpenHashSet<ItemEntry> entries = new ObjectOpenHashSet<>(stacks.size());

		for (var stack : stacks) {
			entries.add(TagEntry.item(stack));
		}

		return entries;
	}
}

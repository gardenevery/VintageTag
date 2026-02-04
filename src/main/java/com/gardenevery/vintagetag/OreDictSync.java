package com.gardenevery.vintagetag;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

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
		ObjectOpenHashSet<ItemKey> keys = new ObjectOpenHashSet<>();

		for (var stack : ores) {
			if (stack == null || stack.isEmpty()) {
				continue;
			}

			try {
				processItemStack(stack, keys);
			} catch (Exception e) {
				//
			}
		}

		if (!keys.isEmpty()) {
			TagManager.registerItem(keys, oreName);
			return keys.size();
		}

		return 0;
	}

	private static void processItemStack(ItemStack stack, ObjectOpenHashSet<ItemKey> keys) {
		if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE && stack.getItem().getHasSubtypes()) {
			var wildcardKeys = syncWildcardEntry(stack.getItem());
			keys.addAll(wildcardKeys);
		} else {
			var key = ItemKey.of(stack);
			keys.add(key);
		}
	}

	private static ObjectOpenHashSet<ItemKey> syncWildcardEntry(Item item) {
		NonNullList<ItemStack> stacks = NonNullList.create();
		item.getSubItems(CreativeTabs.SEARCH, stacks);

		ObjectOpenHashSet<ItemKey> keys = new ObjectOpenHashSet<>(stacks.size());

		for (var stack : stacks) {
			keys.add(ItemKey.of(stack));
		}

		return keys;
	}
}

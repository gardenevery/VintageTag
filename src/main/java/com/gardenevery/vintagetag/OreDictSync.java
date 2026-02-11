package com.gardenevery.vintagetag;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import com.gardenevery.vintagetag.TagEntry.ItemEntry;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

final class OreDictSync {
	private static boolean hasSynced = false;
	private static final Object2ObjectMap<String, ObjectSet<ItemEntry>> ORE_CACHE = new Object2ObjectOpenHashMap<>();

	public static void sync() {
		if (hasSynced) {
			applyCachedTags(false);
			return;
		}

		hasSynced = true;
		syncAllOreDict();
	}

	private static void syncAllOreDict() {
		var oreNames = OreDictionary.getOreNames();

		for (var oreName : oreNames) {
			if (oreName == null || oreName.isEmpty()) {
				continue;
			}

			syncSingleOreDict(oreName);
		}
		applyCachedTags(true);
	}

	private static void syncSingleOreDict(String oreName) {
		var ores = OreDictionary.getOres(oreName, false);
		ObjectSet<ItemEntry> entries = new ObjectOpenHashSet<>();

		for (var stack : ores) {
			if (stack == null || stack.isEmpty()) {
				continue;
			}

			try {
				if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE && stack.getItem().getHasSubtypes()) {
					var wildcardEntries = syncWildcardEntry(stack.getItem());
					entries.addAll(wildcardEntries);
				} else {
					var entry = TagEntry.item(stack);
					entries.add(entry);
				}
			} catch (Exception e) {
				//
			}
		}

		if (!entries.isEmpty()) {
			ORE_CACHE.put(oreName, entries);
		}
	}

	private static void applyCachedTags(boolean showLog) {
		int totalItems = 0;
		int totalTags = ORE_CACHE.size();

		for (Object2ObjectMap.Entry<String, ObjectSet<ItemEntry>> entry : ORE_CACHE.object2ObjectEntrySet()) {
			TagManager.registerItem(entry.getValue(), entry.getKey());
			totalItems += entry.getValue().size();
		}

		if (showLog) {
			TagLog.info("OreDictionary sync completed, {} tags, {} items", totalTags, totalItems);
		}
	}

	private static ObjectSet<ItemEntry> syncWildcardEntry(Item item) {
		NonNullList<ItemStack> stacks = NonNullList.create();
		item.getSubItems(CreativeTabs.SEARCH, stacks);

		ObjectSet<ItemEntry> entries = new ObjectOpenHashSet<>(stacks.size());

		for (var stack : stacks) {
			entries.add(TagEntry.item(stack));
		}

		return entries;
	}
}

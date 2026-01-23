package com.gardenevery.vintagetag;

import java.util.List;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

final class OreSync {

    private static final Object2ObjectMap<String, ObjectSet<ItemKey>> ORE_CACHE = new Object2ObjectOpenHashMap<>();
    private static volatile boolean hasSynced = false;

    public static void oreDictionarySync() {
        if (hasSynced) {
            TagLog.info("=== Using cached OreDictionary data ===");
            applyCachedTags();
            return;
        }

        hasSynced = true;

        var oreNames = OreDictionary.getOreNames();
        int totalEntries = 0;
        int failedEntries = 0;
        TagLog.info("=== Starting sync from OreDictionary to Tags ===");
        TagLog.info("Found {} OreDictionary categories", oreNames.length);

        for (var oreName : oreNames) {
            if (oreName == null || oreName.isEmpty()) {
                continue;
            }

            List<ItemStack> ores = OreDictionary.getOres(oreName, false);
            ObjectSet<ItemKey> itemKeys = new ObjectOpenHashSet<>();

            for (var oreStack : ores) {
                if (oreStack == null || oreStack.isEmpty()) {
                    failedEntries++;
                    continue;
                }

                try {
                    if (oreStack.getMetadata() == OreDictionary.WILDCARD_VALUE && oreStack.getItem().getHasSubtypes()) {
                        ObjectSet<ItemKey> wildcardKeys = syncWildcardEntry(oreStack.getItem());
                        itemKeys.addAll(wildcardKeys);
                        totalEntries += wildcardKeys.size();
                    } else {
                        var key = ItemKey.toKey(oreStack);
                        itemKeys.add(key);
                        totalEntries++;
                    }
                } catch (Exception e) {
                    failedEntries++;
                }
            }
            if (!itemKeys.isEmpty()) {
                ORE_CACHE.put(oreName, itemKeys);
            }
        }

        applyCachedTags();
        TagLog.info("=== Sync completed: {} successful, {} failed ===", totalEntries, failedEntries);
    }

    private static void applyCachedTags() {
        int totalTags = 0;
        for (Object2ObjectMap.Entry<String, ObjectSet<ItemKey>> entry : ORE_CACHE.object2ObjectEntrySet()) {
            TagManager.registerItem(entry.getValue(), entry.getKey());
            totalTags += entry.getValue().size();
        }
        TagLog.info("Applied {} items from {} cached ore categories", totalTags, ORE_CACHE.size());
    }

    private static ObjectSet<ItemKey> syncWildcardEntry(Item item) {
        NonNullList<ItemStack> subItems = NonNullList.create();
        item.getSubItems(CreativeTabs.SEARCH, subItems);

        return subItems.stream()
                .map(ItemKey::toKey)
                .collect(Collectors.toCollection(ObjectOpenHashSet::new));
    }
}

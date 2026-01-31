package com.gardenevery.vintagetag;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

final class OreDictSync {

    private static boolean hasSynced = false;
    private static final Object2ObjectMap<String, ObjectSet<ItemKey>> ORE_CACHE = new Object2ObjectOpenHashMap<>();

    public static void sync() {
        if (hasSynced) {
            applyCachedTags(false);
            return;
        }

        hasSynced = true;
        syncAllOreDictionaryTags();
    }

    private static void syncAllOreDictionaryTags() {
        var oreNames = OreDictionary.getOreNames();

        for (var oreName : oreNames) {
            if (oreName == null || oreName.isEmpty()) {
                continue;
            }

            syncSingleOreDictionaryTag(oreName);
        }

        applyCachedTags(true);
    }

    private static void syncSingleOreDictionaryTag(String oreName) {
        var ores = OreDictionary.getOres(oreName, false);
        ObjectSet<ItemKey> keys = new ObjectOpenHashSet<>();

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
            ORE_CACHE.put(oreName, keys);
        }
    }

    private static void processItemStack(ItemStack stack, ObjectSet<ItemKey> keys) {
        if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE && stack.getItem().getHasSubtypes()) {
            var wildcardKeys = syncWildcardEntry(stack.getItem());
            keys.addAll(wildcardKeys);
        } else {
            var key = ItemKey.of(stack);
            keys.add(key);
        }
    }

    private static void applyCachedTags(boolean showLog) {
        int totalTags = ORE_CACHE.size();
        int totalItems = 0;

        for (Object2ObjectMap.Entry<String, ObjectSet<ItemKey>> entry : ORE_CACHE.object2ObjectEntrySet()) {
            TagManager.registerItem(entry.getValue(), entry.getKey());
            totalItems += entry.getValue().size();
        }

        if (showLog) {
            TagLog.info(
                    "OreDictionary sync completed, {} tags, {} items",
                    totalTags,
                    totalItems
            );
        }
    }

    private static ObjectSet<ItemKey> syncWildcardEntry(Item item) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        item.getSubItems(CreativeTabs.SEARCH, stacks);

        ObjectSet<ItemKey> keys = new ObjectOpenHashSet<>(stacks.size());

        for (var stack : stacks) {
            keys.add(ItemKey.of(stack));
        }

        return keys;
    }
}

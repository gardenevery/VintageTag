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

final class OreSync {
    private static boolean hasSynced = false;
    private static final Object2ObjectMap<String, ObjectSet<ItemKey>> ORE_CACHE = new Object2ObjectOpenHashMap<>();

    public static void sync() {
        if (hasSynced) {
            applyCachedTags(false);
            return;
        }

        hasSynced = true;

        var oreNames = OreDictionary.getOreNames();
        for (var oreName : oreNames) {
            if (oreName == null || oreName.isEmpty()) {
                continue;
            }

            var ores = OreDictionary.getOres(oreName, false);
            ObjectSet<ItemKey> itemKeys = new ObjectOpenHashSet<>();

            for (var oreStack : ores) {
                if (oreStack == null || oreStack.isEmpty()) {
                    continue;
                }

                try {
                    if (oreStack.getMetadata() == OreDictionary.WILDCARD_VALUE && oreStack.getItem().getHasSubtypes()) {
                        var wildcardKeys = syncWildcardEntry(oreStack.getItem());
                        itemKeys.addAll(wildcardKeys);
                    } else {
                        var key = ItemKey.of(oreStack);
                        itemKeys.add(key);
                    }
                } catch (Exception e) {
                    //
                }
            }
            if (!itemKeys.isEmpty()) {
                ORE_CACHE.put(oreName, itemKeys);
            }
        }
        applyCachedTags(true);
    }

    private static void applyCachedTags(boolean showLog) {
        int totalItems = 0;
        for (Object2ObjectMap.Entry<String, ObjectSet<ItemKey>> entry : ORE_CACHE.object2ObjectEntrySet()) {
            TagManager.registerItem(entry.getValue(), entry.getKey());
            totalItems += entry.getValue().size();
        }

        if (showLog) {
            TagLog.info("OreDictionary sync completed, {} tags, {} items", ORE_CACHE.size(), totalItems);
        }
    }

    private static ObjectSet<ItemKey> syncWildcardEntry(Item item) {
        NonNullList<ItemStack> subItems = NonNullList.create();
        item.getSubItems(CreativeTabs.SEARCH, subItems);

        ObjectSet<ItemKey> result = new ObjectOpenHashSet<>(subItems.size());
        for (var stack : subItems) {
            result.add(ItemKey.of(stack));
        }
        return result;
    }
}

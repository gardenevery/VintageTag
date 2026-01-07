package com.gardenevery.vintagetag;

import java.util.List;
import java.util.Set;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

final class OreSync {

    public static void oreDictionarySync() {

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
            for (var oreStack : ores) {
                if (oreStack == null || oreStack.isEmpty()) {
                    failedEntries++;
                    continue;
                }

                try {
                    if (oreStack.getMetadata() == OreDictionary.WILDCARD_VALUE && oreStack.getItem().getHasSubtypes()) {
                        int synced = syncWildcardEntry(oreStack.getItem(), oreName);
                        totalEntries += synced;
                    } else {
                        register(oreStack, oreName);
                        totalEntries++;
                    }
                } catch (Exception e) {
                    failedEntries++;
                }
            }
        }
        TagLog.info("=== Sync completed: {} successful, {} failed ===", totalEntries, failedEntries);
    }

    public static void syncToOreDictionary() {
        TagLog.info("=== Starting sync from Tags to OreDictionary ===");

        int tags = 0;
        int items = 0;

        for (var tagName : TagManager.item().allTags()) {
            if (tagName == null || tagName.isEmpty()) {
                continue;
            }

            Set<ItemKey> itemKeys = TagManager.item().getKeys(tagName);
            if (itemKeys.isEmpty()) {
                continue;
            }

            tags++;

            for (var itemKey : itemKeys) {
                OreDictionary.registerOre(tagName, itemKey.toStack());
                items++;
            }
        }

        OreDictionary.rebakeMap();
        TagLog.info("=== Sync completed: {} tags, {} items ===", tags, items);
    }

    private static int syncWildcardEntry(Item item, String tagName) {
        NonNullList<ItemStack> subItems = NonNullList.create();
        item.getSubItems(CreativeTabs.SEARCH, subItems);

        for (var subStack : subItems) {
            register(subStack, tagName);
        }
        return subItems.size();
    }

    private static void register(ItemStack stack, String tagName) {
        var key = ItemKey.toKey(stack);
        TagManager.item().register(key, tagName);
    }
}

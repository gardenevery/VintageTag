package com.gardenevery.vintagetag;

import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
                if (oreStack.isEmpty()) {
                    failedEntries++;
                    continue;
                }

                try {
                    if (oreStack.getMetadata() == OreDictionary.WILDCARD_VALUE && oreStack.getItem().getHasSubtypes()) {
                        int synced = syncWildcardEntry(oreStack.getItem(), oreName);
                        totalEntries += synced;
                    } else {
                        create(oreStack, oreName);
                        totalEntries++;
                    }
                } catch (Exception e) {
                    failedEntries++;
                }
            }
        }
        TagLog.info("=== Sync completed: {} successful, {} failed ===", totalEntries, failedEntries);
    }

    private static int syncWildcardEntry(Item item, String tagName) {
        int synced = 0;
        for (int meta = 0; meta < 16; meta++) {
            try {
                var specificStack = new ItemStack(item, 1, meta);
                    create(specificStack, tagName);
                    synced++;
            } catch (Exception e) {
                //
            }
        }

        if (synced == 0) {
            var wildcardStack = new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE);
            create(wildcardStack, tagName);
            synced = 1;
        }
        return synced;
    }

    public static void syncToOreDictionary() {
        TagLog.info("=== Starting sync from Tags to OreDictionary ===");

        int tags = 0;
        int items = 0;

        for (var tagName : TagManager.ITEM.allTags()) {
            if (tagName == null || tagName.isEmpty()) {
                continue;
            }

            Set<ItemKey> itemKeys = TagManager.ITEM.getKeys(tagName);
            if (itemKeys.isEmpty()) {
                continue;
            }

            tags++;

            for (var itemKey : itemKeys) {
                try {
                    var stack = itemKey.toElement();
                    if (!stack.isEmpty()) {
                        OreDictionary.registerOre(tagName, stack);
                        items++;
                    }
                } catch (Exception ignored) {
                    //
                }
            }
        }

        OreDictionary.rebakeMap();
        TagLog.info("=== Sync completed: {} tags, {} items ===", tags, items);
    }

    private static void create(ItemStack stack, String tagName) {
        if (stack == null || stack.isEmpty() || tagName == null || tagName.isEmpty()) {
            return;
        }

        var key = ItemKey.toKey(stack);
        TagManager.ITEM.create(key, tagName);
    }
}

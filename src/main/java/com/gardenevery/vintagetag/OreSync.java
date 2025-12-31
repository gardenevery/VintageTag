package com.gardenevery.vintagetag;

import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class OreSync {

    private static final Logger LOGGER = LogManager.getLogger("VintageTag");

    public static void oreDictionarySync() {
        var oreNames = OreDictionary.getOreNames();
        int totalEntries = 0;
        int failedEntries = 0;
        LOGGER.info("=== Starting sync from OreDictionary to Tags ===");
        LOGGER.info("Found {} OreDictionary categories", oreNames.length);

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
                        create(oreName, oreStack);
                        totalEntries++;
                    }
                } catch (Exception e) {
                    failedEntries++;
                }
            }
        }
        LOGGER.info("=== Sync completed: {} successful, {} failed ===", totalEntries, failedEntries);
    }

    private static int syncWildcardEntry(Item item, String tagName) {
        int synced = 0;
        for (int meta = 0; meta < 16; meta++) {
            try {
                var specificStack = new ItemStack(item, 1, meta);
                if (!specificStack.isEmpty() && specificStack.getItem() == item) {
                    create(tagName, specificStack);
                    synced++;
                }
            } catch (Exception e) {
                //
            }
        }

        if (synced == 0) {
            var wildcardStack = new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE);
            create(tagName, wildcardStack);
            synced = 1;
        }
        return synced;
    }

    public static void syncToOreDictionary() {
        LOGGER.info("=== Starting sync from Tags to OreDictionary ===");

        int tags = 0;
        int items = 0;

        for (var tagName : TagManager.ITEM.getAllTags()) {
            if (tagName == null || tagName.isEmpty()) {
                continue;
            }

            Set<ItemKey> itemKeys = TagManager.ITEM.getKey(tagName);
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
        LOGGER.info("=== Sync completed: {} tags, {} items ===", tags, items);
    }

    private static void create(String tagName, ItemStack stack) {
        if (tagName == null || tagName.isEmpty() || stack == null || stack.isEmpty()) {
            return;
        }

        var key = ItemKey.toKey(stack);
        TagManager.ITEM.create(tagName, key);
    }
}

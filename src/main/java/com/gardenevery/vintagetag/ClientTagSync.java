package com.gardenevery.vintagetag;

import com.gardenevery.vintagetag.TagSync.SyncType;
import com.gardenevery.vintagetag.TagSync.TagDataSyncMessage;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@SideOnly(Side.CLIENT)
final class ClientTagSync {
    @SideOnly(Side.CLIENT)
    public static void register() {
        if (TagSync.NETWORK == null) {
            TagSync.NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("VintageTag");
        }
        TagSync.NETWORK.registerMessage(TagDataSyncHandler.class, TagDataSyncMessage.class, 0, Side.CLIENT);
    }

    @SideOnly(Side.CLIENT)
    public static class TagDataSyncHandler implements IMessageHandler<TagDataSyncMessage, IMessage> {
        @Override
        public IMessage onMessage(TagDataSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> processClientSync(message));
            return null;
        }

        private void processClientSync(TagDataSyncMessage message) {
            if (message == null || message.tagData == null || message.type == null || message.type == SyncType.NONE) {
                return;
            }

            processAllTags(message);
            TagManager.bake();
        }

        private void processAllTags(TagDataSyncMessage message) {
            for (var entry : message.tagData.itemTags().object2ObjectEntrySet()) {
                processItemEntries(entry.getKey(), entry.getValue());
            }

            for (var entry : message.tagData.fluidTags().object2ObjectEntrySet()) {
                processFluidEntries(entry.getKey(), entry.getValue());
            }

            for (var entry : message.tagData.blockTags().object2ObjectEntrySet()) {
                processBlockEntries(entry.getKey(), entry.getValue());
            }
        }

        private void processItemEntries(String tagName, IntArrayList entries) {
            if (entries.isEmpty()) {
                return;
            }

            ObjectSet<ItemKey> results = transformItemEntries(entries);
            if (!results.isEmpty()) {
                TagManager.registerItem(results, tagName);
            }
        }

        private void processFluidEntries(String tagName, ObjectArrayList<String> entries) {
            if (entries.isEmpty()) {
                return;
            }

            ObjectSet<Fluid> results = transformFluidEntries(entries);
            if (!results.isEmpty()) {
                TagManager.registerFluid(results, tagName);
            }
        }

        private void processBlockEntries(String tagName, IntArrayList entries) {
            if (entries.isEmpty()) {
                return;
            }

            ObjectSet<Block> results = transformBlockEntries(entries);
            if (!results.isEmpty()) {
                TagManager.registerBlock(results, tagName);
            }
        }

        private ObjectSet<ItemKey> transformItemEntries(IntArrayList entries) {
            ObjectSet<ItemKey> results = new ObjectOpenHashSet<>(entries.size() / 2);
            for (int i = 0; i < entries.size(); i += 2) {
                var key = createItemKey(entries.getInt(i), entries.getInt(i + 1));
                results.add(key);
            }
            return results;
        }

        private ItemKey createItemKey(int id, int metadata) {
            if (id < 0) {
                return null;
            }

            var item = Item.getItemById(id);
            return new ItemKey(item, metadata);
        }

        private ObjectSet<Fluid> transformFluidEntries(ObjectArrayList<String> entries) {
            ObjectSet<Fluid> results = new ObjectOpenHashSet<>(entries.size());
            for (var fluidName : entries) {
                var fluid = FluidRegistry.getFluid(fluidName);
                if (fluid != null) {
                    results.add(fluid);
                }
            }
            return results;
        }

        private ObjectSet<Block> transformBlockEntries(IntArrayList entries) {
            ObjectSet<Block> results = new ObjectOpenHashSet<>(entries.size());
            for (int id : entries) {
                if (id >= 0) {
                    var block = Block.getBlockById(id);
                    results.add(block);
                }
            }
            return results;
        }
    }
}

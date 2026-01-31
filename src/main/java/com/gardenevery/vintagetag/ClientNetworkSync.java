package com.gardenevery.vintagetag;

import com.gardenevery.vintagetag.NetworkSync.SyncType;
import com.gardenevery.vintagetag.NetworkSync.TagDataSyncMessage;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@SideOnly(Side.CLIENT)
final class ClientNetworkSync {

    @SideOnly(Side.CLIENT)
    public static void register() {
        if (NetworkSync.NETWORK == null) {
            NetworkSync.NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("VintageTag");
        }
        NetworkSync.NETWORK.registerMessage(
                TagDataSyncHandler.class,
                TagDataSyncMessage.class,
                0,
                Side.CLIENT
        );
    }

    @SideOnly(Side.CLIENT)
    public static class TagDataSyncHandler implements IMessageHandler<TagDataSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(TagDataSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> processClientSync(message));
            return null;
        }

        private void processClientSync(TagDataSyncMessage message) {
            if (
                    message == null ||
                            message.tagData == null ||
                            message.type == null ||
                            message.type == SyncType.NONE
            ) {
                return;
            }

            processAllTags(message);
            TagManager.bake();
        }

        private void processAllTags(TagDataSyncMessage message) {
            for (var itemEntry : message.tagData.itemTags().object2ObjectEntrySet()) {
                processItemEntries(itemEntry.getKey(), itemEntry.getValue());
            }

            for (var fluidEntry : message.tagData.fluidTags().object2ObjectEntrySet()) {
                processFluidEntries(fluidEntry.getKey(), fluidEntry.getValue());
            }

            for (var blockEntry : message.tagData.blockTags().object2ObjectEntrySet()) {
                processBlockEntries(blockEntry.getKey(), blockEntry.getValue());
            }
        }

        private void processItemEntries(String tagName, IntArrayList entries) {
            if (entries.isEmpty()) {
                return;
            }

            ObjectSet<ItemKey> keys = transformItemEntries(entries);
            if (!keys.isEmpty()) {
                TagManager.registerItem(keys, tagName);
            }
        }

        private void processFluidEntries(String tagName, ObjectArrayList<String> entries) {
            if (entries.isEmpty()) {
                return;
            }

            ObjectSet<Fluid> fluids = transformFluidEntries(entries);
            if (!fluids.isEmpty()) {
                TagManager.registerFluid(fluids, tagName);
            }
        }

        private void processBlockEntries(String tagName, IntArrayList entries) {
            if (entries.isEmpty()) {
                return;
            }

            ObjectSet<Block> blocks = transformBlockEntries(entries);
            if (!blocks.isEmpty()) {
                TagManager.registerBlock(blocks, tagName);
            }
        }

        private ObjectSet<ItemKey> transformItemEntries(IntArrayList entries) {
            ObjectSet<ItemKey> keys = new ObjectOpenHashSet<>(entries.size() / 2);

            for (int i = 0; i < entries.size(); i += 2) {
                var key = createItemKey(entries.getInt(i), entries.getInt(i + 1));
                if (key != null) {
                    keys.add(key);
                }
            }

            return keys;
        }

        private ItemKey createItemKey(int id, int metadata) {
            if (id < 0) {
                return null;
            }

            var item = Item.getItemById(id);
            var stack = new ItemStack(item, 1, metadata);
            return ItemKey.of(stack);
        }

        private ObjectSet<Fluid> transformFluidEntries(ObjectArrayList<String> entries) {
            ObjectSet<Fluid> fluids = new ObjectOpenHashSet<>(entries.size());

            for (var fluidName : entries) {
                var fluid = FluidRegistry.getFluid(fluidName);
                if (fluid != null) {
                    fluids.add(fluid);
                }
            }

            return fluids;
        }

        private ObjectSet<Block> transformBlockEntries(IntArrayList entries) {
            ObjectSet<Block> blocks = new ObjectOpenHashSet<>(entries.size());

            for (int id : entries) {
                if (id >= 0) {
                    var block = Block.getBlockById(id);
                    blocks.add(block);
                }
            }

            return blocks;
        }
    }
}

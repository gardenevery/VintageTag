package com.gardenevery.vintagetag;

import com.gardenevery.vintagetag.TagSync.ClientSyncMessage;
import com.gardenevery.vintagetag.TagSync.TagDataSyncMessage;
import com.gardenevery.vintagetag.TagSync.ServerSyncMessage;
import com.gardenevery.vintagetag.TagSync.SyncType;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@SideOnly(Side.CLIENT)
final class ClientTagSync {

    @SideOnly(Side.CLIENT)
    public static void registerClient() {
        if (TagSync.NETWORK == null) {
            TagSync.NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("TagSync");
        }

        TagSync.NETWORK.registerMessage(TagDataSyncHandler.class, TagDataSyncMessage.class, 0, Side.CLIENT);
        TagSync.NETWORK.registerMessage(ServerSyncHandler.class, ServerSyncMessage.class, 1, Side.CLIENT);
        TagSync.NETWORK.registerMessage(ClientSyncHandler.class, ClientSyncMessage.class, 2, Side.SERVER);
    }

    @SideOnly(Side.CLIENT)
    public static class TagDataSyncHandler implements IMessageHandler<TagDataSyncMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(TagDataSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> processClientSync(message));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void processClientSync(TagDataSyncMessage message) {
            if (message == null || message.tagData == null) {
                return;
            }

            if (message.syncType == SyncType.FULL) {
                TagManager.ITEM.clear();
                TagManager.FLUID.clear();
                TagManager.BLOCK.clear();

                for (var entry : message.tagData.itemTags.object2ObjectEntrySet()) {
                    ObjectSet<ItemKey> keys = new ObjectOpenHashSet<>(entry.getValue().size());
                    for (var itemEntry : entry.getValue()) {
                        try {
                            var location = new ResourceLocation(itemEntry.itemId());
                            var item = ForgeRegistries.ITEMS.getValue(location);
                            if (item != null) {
                                keys.add(new ItemKey(item, itemEntry.metadata()));
                            }
                        } catch (Exception e) {
                            //
                        }
                    }
                    if (!keys.isEmpty()) {
                        TagManager.ITEM.create(keys, entry.getKey());
                    }
                }

                for (var entry : message.tagData.fluidTags.object2ObjectEntrySet()) {
                    ObjectSet<Fluid> fluids = new ObjectOpenHashSet<>(entry.getValue().size());
                    for (var fluidName : entry.getValue()) {
                        var fluid = FluidRegistry.getFluid(fluidName);
                        if (fluid != null) {
                            fluids.add(fluid);
                        }
                    }
                    if (!fluids.isEmpty()) {
                        TagManager.FLUID.create(fluids, entry.getKey());
                    }
                }

                for (var entry : message.tagData.blockTags.object2ObjectEntrySet()) {
                    ObjectSet<Block> blocks = new ObjectOpenHashSet<>(entry.getValue().size());
                    for (var blockName : entry.getValue()) {
                        try {
                            var location = new ResourceLocation(blockName);
                            var block = ForgeRegistries.BLOCKS.getValue(location);
                            if (block != null) {
                                blocks.add(block);
                            }
                        } catch (Exception e) {
                            //
                        }
                    }
                    if (!blocks.isEmpty()) {
                        TagManager.BLOCK.create(blocks, entry.getKey());
                    }
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ServerSyncHandler implements IMessageHandler<ServerSyncMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(ServerSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(this::processOreDictionarySync);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void processOreDictionarySync() {
            OreSync.syncToOreDictionary();
        }
    }

    @SideOnly(Side.CLIENT)
    public static class ClientSyncHandler implements IMessageHandler<ClientSyncMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(ClientSyncMessage message, MessageContext ctx) {
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void processOreDictionarySync() {}
    }
}

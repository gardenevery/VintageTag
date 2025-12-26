package com.gardenevery.vintagetag;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public final class ClientTagSync {

    private ClientTagSync() {}

    @SideOnly(Side.CLIENT)
    public static void registerClient() {
        if (TagSync.NETWORK == null) {
            TagSync.NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("TagSync");
        }
        TagSync.NETWORK.registerMessage(ClientSyncHandler.class, TagSync.ClientSyncMessage.class, 0, Side.CLIENT);
    }

    @SideOnly(Side.CLIENT)
    public static class ClientSyncHandler implements IMessageHandler<TagSync.ClientSyncMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(TagSync.ClientSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> processClientSync(message));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void processClientSync(TagSync.ClientSyncMessage message) {
            if (message == null || message.tagData == null) {
                return;
            }

            if (message.syncType == TagSync.SyncType.FULL) {
                TagHelper.cleanAllTag();

                for (Map.Entry<String, List<TagSync.ItemEntry>> entry : message.tagData.itemTags.entrySet()) {
                    Set<ItemKey> keys = new HashSet<>();
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
                        TagManager.ITEM.createTag(entry.getKey(), keys);
                    }
                }

                for (Map.Entry<String, List<String>> entry : message.tagData.fluidTags.entrySet()) {
                    Set<Fluid> fluids = new HashSet<>();
                    for (var fluidName : entry.getValue()) {
                        var fluid = FluidRegistry.getFluid(fluidName);
                        if (fluid != null) {
                            fluids.add(fluid);
                        }
                    }
                    if (!fluids.isEmpty()) {
                        TagManager.FLUID.createTag(entry.getKey(), fluids);
                    }
                }

                for (Map.Entry<String, List<String>> entry : message.tagData.blockTags.entrySet()) {
                    Set<Block> blocks = new HashSet<>();
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
                        TagManager.BLOCK.createTag(entry.getKey(), blocks);
                    }
                }
            }
        }
    }
}

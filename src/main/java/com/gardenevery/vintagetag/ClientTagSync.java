package com.gardenevery.vintagetag;

import com.gardenevery.vintagetag.TagSync.TagDataSyncMessage;
import com.gardenevery.vintagetag.TagSync.SyncType;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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
            TagSync.NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("VintageTag");
        }
        TagSync.NETWORK.registerMessage(TagDataSyncHandler.class, TagDataSyncMessage.class, 0, Side.CLIENT);
    }

    @SideOnly(Side.CLIENT)
    public static class TagDataSyncHandler implements IMessageHandler<TagDataSyncMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(TagDataSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> processClientSync(message));
            return null;
        }

        @SuppressWarnings("deprecation")
        @SideOnly(Side.CLIENT)
        private void processClientSync(TagDataSyncMessage message) {
            if (message == null || message.tagData == null || message.type == null) {
                return;
            }

            if (message.type == SyncType.FULL) {
                for (var entry : message.tagData.itemTags.object2ObjectEntrySet()) {
                    ObjectSet<ItemKey> itemKeys = new ObjectOpenHashSet<>(entry.getValue().size());
                    for (var itemEntry : entry.getValue()) {
                        try {
                            var resourceLocation = new ResourceLocation(itemEntry.itemId());
                            var item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                            if (item != null) {
                                itemKeys.add(new ItemKey(item, itemEntry.metadata()));
                            }
                        } catch (Exception e) {
                            //
                        }
                    }
                    if (!itemKeys.isEmpty()) {
                        TagManager.registerItem(itemKeys, entry.getKey());
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
                        TagManager.registerFluid(fluids, entry.getKey());
                    }
                }

                for (var entry : message.tagData.blockTags.object2ObjectEntrySet()) {
                    ObjectSet<Block> blocks = new ObjectOpenHashSet<>(entry.getValue().size());
                    for (var blockName : entry.getValue()) {
                        try {
                            var resourceLocation = new ResourceLocation(blockName);
                            var block = ForgeRegistries.BLOCKS.getValue(resourceLocation);
                            if (block != null) {
                                blocks.add(block);
                            }
                        } catch (Exception e) {
                            //
                        }
                    }
                    if (!blocks.isEmpty()) {
                        TagManager.registerBlock(blocks, entry.getKey());
                    }
                }

                for (var entry : message.tagData.blockStateTags.object2ObjectEntrySet()) {
                    ObjectSet<IBlockState> blockStates = new ObjectOpenHashSet<>(entry.getValue().size());
                    for (var blockStateEntry : entry.getValue()) {
                        try {
                            var resourceLocation = new ResourceLocation(blockStateEntry.blockId());
                            var block = ForgeRegistries.BLOCKS.getValue(resourceLocation);
                            if (block != null) {
                                var blockState = block.getStateFromMeta(blockStateEntry.metadata());
                                blockStates.add(blockState);
                            }
                        } catch (Exception e) {
                            //
                        }
                    }
                    if (!blockStates.isEmpty()) {
                        TagManager.registerBlockState(blockStates, entry.getKey());
                    }
                }
                TagManager.bake();
            }
        }
    }
}

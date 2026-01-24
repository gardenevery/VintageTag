package com.gardenevery.vintagetag;

import java.util.function.Function;

import com.gardenevery.vintagetag.TagSync.BlockStateEntry;
import com.gardenevery.vintagetag.TagSync.ItemEntry;
import com.gardenevery.vintagetag.TagSync.SyncType;
import com.gardenevery.vintagetag.TagSync.TagDataSyncMessage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
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

        @SideOnly(Side.CLIENT)
        private void processClientSync(TagDataSyncMessage message) {
            if (message == null || message.tagData == null || message.type == null) {
                return;
            }

            if (message.type == SyncType.NONE) {
                return;
            }

            message.tagData.itemTags().object2ObjectEntrySet()
                    .forEach(entry -> processItemEntries(entry.getKey(), entry.getValue()));

            message.tagData.fluidTags().object2ObjectEntrySet()
                    .forEach(entry -> processFluidEntries(entry.getKey(), entry.getValue()));

            message.tagData.blockTags().object2ObjectEntrySet()
                    .forEach(entry -> processBlockEntries(entry.getKey(), entry.getValue()));

            message.tagData.blockStateTags().object2ObjectEntrySet()
                    .forEach(entry -> processBlockStateEntries(entry.getKey(), entry.getValue()));

            TagManager.bake();
        }

        @SideOnly(Side.CLIENT)
        private void processItemEntries(String tagName, ObjectArrayList<ItemEntry> entries) {
            processEntries(tagName, entries, entry -> {
                    try {
                        var resourceLocation = new ResourceLocation(entry.itemId());
                        var item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                        return item != null ? new ItemKey(item, entry.metadata()) : null;
                    } catch (Exception e) {
                        return null;
                    }
                },
                TagManager::registerItem
            );
        }

        @SideOnly(Side.CLIENT)
        private void processFluidEntries(String tagName, ObjectArrayList<String> entries) {
            processEntries(tagName, entries, FluidRegistry::getFluid, TagManager::registerFluid);
        }

        @SideOnly(Side.CLIENT)
        private void processBlockEntries(String tagName, ObjectArrayList<String> entries) {
            processEntries(tagName, entries, name -> {
                   try {
                       var resourceLocation = new ResourceLocation(name);
                       return ForgeRegistries.BLOCKS.getValue(resourceLocation);
                   } catch (Exception e) {
                       return null;
                   }
               },
               TagManager::registerBlock
            );
        }

        @SuppressWarnings("deprecation")
        @SideOnly(Side.CLIENT)
        private void processBlockStateEntries(String tagName, ObjectArrayList<BlockStateEntry> entries) {
            processEntries(tagName, entries, entry -> {
                    try {
                        var resourceLocation = new ResourceLocation(entry.blockId());
                        var block = ForgeRegistries.BLOCKS.getValue(resourceLocation);
                        return block != null ? block.getStateFromMeta(entry.metadata()) : null;
                    } catch (Exception e) {
                        return null;
                    }
                },
                TagManager::registerBlockState
            );
        }

        @SideOnly(Side.CLIENT)
        private <T, R> void processEntries(String tagName, ObjectArrayList<T> entries, Function<T, R> mapper, RegistrationFunction<R> registerFunc) {
            if (entries == null || entries.isEmpty()) {
                return;
            }

            ObjectSet<R> results = new ObjectOpenHashSet<>(entries.size());
            entries.forEach(entry -> {
                R result = mapper.apply(entry);
                if (result != null) {
                    results.add(result);
                }
            });

            if (!results.isEmpty()) {
                registerFunc.register(results, tagName);
            }
        }

        @FunctionalInterface
        @SideOnly(Side.CLIENT)
        private interface RegistrationFunction<T> {
            void register(ObjectSet<T> items, String tagName);
        }
    }
}

package com.gardenevery.vintagetag;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public static void registerClient() {
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
            message.tagData.itemTags().object2ObjectEntrySet()
                    .forEach(entry -> processItemEntries(entry.getKey(), entry.getValue()));
            message.tagData.fluidTags().object2ObjectEntrySet()
                    .forEach(entry -> processFluidEntries(entry.getKey(), entry.getValue()));
            message.tagData.blockTags().object2ObjectEntrySet()
                    .forEach(entry -> processBlockEntries(entry.getKey(), entry.getValue()));
        }

        private void processItemEntries(String tagName, IntArrayList entries) {
            processTagEntries(entries, this::transformItemEntries,
                    results -> TagManager.registerItem(results, tagName));
        }

        private void processFluidEntries(String tagName, ObjectArrayList<String> entries) {
            processTagEntries(entries, this::transformFluidEntries,
                    results -> TagManager.registerFluid(results, tagName));
        }

        private void processBlockEntries(String tagName, IntArrayList entries) {
            processTagEntries(entries, this::transformBlockEntries,
                    results -> TagManager.registerBlock(results, tagName));
        }

        private ObjectSet<ItemKey> transformItemEntries(IntArrayList entries) {
            return IntStream.range(0, entries.size() / 2)
                    .mapToObj(i -> createItemKey(entries.getInt(i * 2), entries.getInt(i * 2 + 1)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ObjectOpenHashSet::new));
        }

        private ItemKey createItemKey(int itemId, int metadata) {
            var item = Item.getItemById(itemId);
            return itemId >= 0 ? new ItemKey(item, metadata) : null;
        }

        private ObjectSet<Fluid> transformFluidEntries(ObjectArrayList<String> entries) {
            ObjectSet<Fluid> results = new ObjectOpenHashSet<>(entries.size());
            entries.forEach(fluidName -> {
                var fluid = FluidRegistry.getFluid(fluidName);
                if (fluid != null) {
                    results.add(fluid);
                }
            });
            return results;
        }

        private ObjectSet<Block> transformBlockEntries(IntArrayList entries) {
            ObjectSet<Block> results = new ObjectOpenHashSet<>(entries.size());
            entries.forEach(blockId -> {
                var block = Block.getBlockById(blockId);
                if (blockId >= 0) {
                    results.add(block);
                }
            });
            return results;
        }

        private <T, R> void processTagEntries(T entries, Function<T, ObjectSet<R>> transformer, Consumer<ObjectSet<R>> registerAction) {
            if (isEmpty(entries)) {
                return;
            }

            var results = transformer.apply(entries);
            if (!results.isEmpty()) {
                registerAction.accept(results);
            }
        }

        private <T> boolean isEmpty(T collection) {
            if (collection instanceof IntArrayList intList) {
                return intList.isEmpty();
            }

            if (collection instanceof ObjectArrayList<?> objList) {
                return objList.isEmpty();
            }
            return true;
        }
    }
}

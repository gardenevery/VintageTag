package com.gardenevery.vintagetag;

import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

import com.github.bsideup.jabel.Desugar;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

final class TagSync {
    public static SimpleNetworkWrapper NETWORK;
    private static final int MAX_PACKET_SIZE = 2 * 1024 * 1024;
    private static final int INVALID_ID = -1;

    public enum SyncType {
        NONE,
        FULL
    }

    public static void register() {
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("VintageTag");
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        NETWORK.registerMessage((message, ctx) -> null, TagDataSyncMessage.class, 0, Side.CLIENT);
    }

    public static void sync(@Nullable EntityPlayerMP player) {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) {
            return;
        }

        var tagData = collectTagData();
        var message = new TagDataSyncMessage(SyncType.FULL, tagData);

        if (player == null) {
            for (var onlinePlayer : FMLCommonHandler.instance()
                    .getMinecraftServerInstance()
                    .getPlayerList()
                    .getPlayers()) {
                NETWORK.sendTo(message, onlinePlayer);
            }
        } else {
            NETWORK.sendTo(message, player);
        }
    }

    private static TagData collectTagData() {
        return new TagData(collectItemTags(), collectFluidTags(), collectBlockTags());
    }

    private static Object2ObjectMap<String, IntArrayList> collectItemTags() {
        Object2ObjectMap<String, IntArrayList> itemTags = new Object2ObjectOpenHashMap<>();

        for (var tagName : TagManager.item().getAllTags()) {
            var keys = TagManager.item().getKeys(tagName);
            var entries = new IntArrayList(keys.size() * 2);

            for (var key : keys) {
                int itemId = getItemId(key.item());
                if (itemId != INVALID_ID) {
                    entries.add(itemId);
                    entries.add(key.metadata());
                }
            }

            if (!entries.isEmpty()) {
                itemTags.put(tagName, entries);
            }
        }
        return itemTags;
    }

    private static Object2ObjectMap<String, ObjectArrayList<String>> collectFluidTags() {
        Object2ObjectMap<String, ObjectArrayList<String>> fluidTags = new Object2ObjectOpenHashMap<>();

        for (var tagName : TagManager.fluid().getAllTags()) {
            var fluids = TagManager.fluid().getKeys(tagName);
            ObjectArrayList<String> fluidNames = new ObjectArrayList<>(fluids.size());

            for (var fluid : fluids) {
                var fluidName = FluidRegistry.getFluidName(fluid);
                if (fluidName != null) {
                    fluidNames.add(fluidName);
                }
            }

            if (!fluidNames.isEmpty()) {
                fluidTags.put(tagName, fluidNames);
            }
        }
        return fluidTags;
    }

    private static Object2ObjectMap<String, IntArrayList> collectBlockTags() {
        Object2ObjectMap<String, IntArrayList> blockTags = new Object2ObjectOpenHashMap<>();

        for (var tagName : TagManager.block().getAllTags()) {
            var blocks = TagManager.block().getKeys(tagName);
            var blockIds = new IntArrayList(blocks.size());

            for (var block : blocks) {
                int blockId = getBlockId(block);
                if (blockId != INVALID_ID) {
                    blockIds.add(blockId);
                }
            }

            if (!blockIds.isEmpty()) {
                blockTags.put(tagName, blockIds);
            }
        }
        return blockTags;
    }

    private static int getItemId(Item item) {
        if (item == null) {
            return INVALID_ID;
        }

        int id = Item.getIdFromItem(item);
        return id >= 0 ? id : INVALID_ID;
    }

    private static int getBlockId(Block block) {
        if (block == null) {
            return INVALID_ID;
        }

        int id = Block.getIdFromBlock(block);
        return id >= 0 ? id : INVALID_ID;
    }

    public static class EventHandler {
        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP player) {
                TagSync.sync(player);
            }
        }
    }

    public static class TagDataSyncMessage implements IMessage {
        public SyncType type;
        public TagData tagData;

        public TagDataSyncMessage() {}

        public TagDataSyncMessage(SyncType type, TagData tagData) {
            this.type = type;
            this.tagData = tagData;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            int totalSize = buf.readableBytes();
            validateSize(totalSize);

            int st = buf.readUnsignedByte();
            type = readSyncType(st);

            Object2ObjectMap<String, IntArrayList> itemTags = readOptionalMap(buf, this::readItemTags);
            Object2ObjectMap<String, ObjectArrayList<String>> fluidTags = readOptionalMap(buf, this::readFluidTags);
            Object2ObjectMap<String, IntArrayList> blockTags = readOptionalMap(buf, this::readBlockTags);

            tagData = new TagData(itemTags, fluidTags, blockTags);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            var tempBuf = buf.alloc().buffer();
            try {
                tempBuf.writeByte(type.ordinal());

                writeOptionalMap(tempBuf, tagData.itemTags(), this::writeItemTags);
                writeOptionalMap(tempBuf, tagData.fluidTags(), this::writeFluidTags);
                writeOptionalMap(tempBuf, tagData.blockTags(), this::writeBlockTags);

                int totalSize = tempBuf.readableBytes();
                validateSize(totalSize);

                buf.writeBytes(tempBuf);
            } finally {
                tempBuf.release();
            }
        }

        private <T> Object2ObjectMap<String, T> readOptionalMap(ByteBuf buf, MapReader<T> reader) {
            if (!buf.readBoolean()) {
                return new Object2ObjectOpenHashMap<>();
            }

            int tagCount = buf.readInt();
            validateCount(tagCount);
            return reader.read(buf, tagCount);
        }

        private <T> void writeOptionalMap(ByteBuf buf, Object2ObjectMap<String, T> map, MapWriter<T> writer) {
            boolean hasMap = map != null && !map.isEmpty();
            buf.writeBoolean(hasMap);

            if (hasMap) {
                buf.writeInt(map.size());
                writer.write(buf, map);
            }
        }

        private Object2ObjectMap<String, IntArrayList> readItemTags(ByteBuf buf, int tagCount) {
            Object2ObjectMap<String, IntArrayList> map = new Object2ObjectOpenHashMap<>(tagCount);

            for (int i = 0; i < tagCount; i++) {
                var tagName = readString(buf);
                int entryCount = buf.readInt();
                validateCount(entryCount);

                var entries = new IntArrayList(entryCount * 2);
                for (int j = 0; j < entryCount; j++) {
                    int itemId = buf.readInt();
                    int metadata = buf.readInt();
                    entries.add(itemId);
                    entries.add(metadata);
                }
                map.put(tagName, entries);
            }
            return map;
        }

        private Object2ObjectMap<String, ObjectArrayList<String>> readFluidTags(ByteBuf buf, int tagCount) {
            Object2ObjectMap<String, ObjectArrayList<String>> map = new Object2ObjectOpenHashMap<>(tagCount);

            for (int i = 0; i < tagCount; i++) {
                var tagName = readString(buf);
                int entryCount = buf.readInt();
                validateCount(entryCount);

                ObjectArrayList<String> fluidNames = new ObjectArrayList<>(entryCount);
                for (int j = 0; j < entryCount; j++) {
                    var fluidName = readString(buf);
                    fluidNames.add(fluidName);
                }
                map.put(tagName, fluidNames);
            }
            return map;
        }

        private Object2ObjectMap<String, IntArrayList> readBlockTags(ByteBuf buf, int tagCount) {
            Object2ObjectMap<String, IntArrayList> map = new Object2ObjectOpenHashMap<>(tagCount);

            for (int i = 0; i < tagCount; i++) {
                var tagName = readString(buf);
                int entryCount = buf.readInt();
                validateCount(entryCount);

                var blockIds = new IntArrayList(entryCount);
                for (int j = 0; j < entryCount; j++) {
                    int blockId = buf.readInt();
                    blockIds.add(blockId);
                }
                map.put(tagName, blockIds);
            }
            return map;
        }

        private void writeItemTags(ByteBuf buf, Object2ObjectMap<String, IntArrayList> map) {
            for (var entry : map.object2ObjectEntrySet()) {
                writeString(buf, entry.getKey());
                var itemEntries = entry.getValue();
                int pairCount = itemEntries.size() / 2;
                buf.writeInt(pairCount);

                for (int i = 0; i < pairCount; i++) {
                    int itemId = itemEntries.getInt(i * 2);
                    int metadata = itemEntries.getInt(i * 2 + 1);
                    buf.writeInt(itemId);
                    buf.writeInt(metadata);
                }
            }
        }

        private void writeFluidTags(ByteBuf buf, Object2ObjectMap<String, ObjectArrayList<String>> map) {
            for (var entry : map.object2ObjectEntrySet()) {
                writeString(buf, entry.getKey());
                var fluidNames = entry.getValue();
                buf.writeInt(fluidNames.size());

                for (var fluidName : fluidNames) {
                    writeString(buf, fluidName);
                }
            }
        }

        private void writeBlockTags(ByteBuf buf, Object2ObjectMap<String, IntArrayList> map) {
            for (var entry : map.object2ObjectEntrySet()) {
                writeString(buf, entry.getKey());
                var blockIds = entry.getValue();
                buf.writeInt(blockIds.size());

                for (int blockId : blockIds) {
                    buf.writeInt(blockId);
                }
            }
        }

        private void validateSize(int size) {
            if (size > MAX_PACKET_SIZE) {
                TagLog.error("Packet too large: {} bytes, max allowed: {}", size, MAX_PACKET_SIZE);
            }
        }

        private void validateCount(int count) {
            if (count < 0) {
                TagLog.error("Invalid count: {}", count);
            }
        }

        private SyncType readSyncType(int ordinal) {
            if (ordinal < 0 || ordinal >= SyncType.values().length) {
                return SyncType.NONE;
            }
            return SyncType.values()[ordinal];
        }

        private String readString(ByteBuf buf) {
            int length = buf.readInt();
            if (length < 0) {
                TagLog.error("Negative string length: {}", length);
                return "";
            }

            if (buf.readableBytes() < length) {
                TagLog.error("Not enough bytes for string: need {}, have {}", length, buf.readableBytes());
                return "";
            }

            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private void writeString(ByteBuf buf, String string) {
            if (string == null || string.isEmpty()) {
                buf.writeInt(0);
                return;
            }

            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }

        @FunctionalInterface
        private interface MapReader<T> {
            Object2ObjectMap<String, T> read(ByteBuf buf, int tagCount);
        }

        @FunctionalInterface
        private interface MapWriter<T> {
            void write(ByteBuf buf, Object2ObjectMap<String, T> map);
        }
    }

    @Desugar
    public record TagData(Object2ObjectMap<String, IntArrayList> itemTags,
                          Object2ObjectMap<String, ObjectArrayList<String>> fluidTags,
                          Object2ObjectMap<String, IntArrayList> blockTags) {
        public TagData(
                Object2ObjectMap<String, IntArrayList> itemTags,
                Object2ObjectMap<String, ObjectArrayList<String>> fluidTags,
                Object2ObjectMap<String, IntArrayList> blockTags
        ) {
            this.itemTags = itemTags != null ? itemTags : new Object2ObjectOpenHashMap<>();
            this.fluidTags = fluidTags != null ? fluidTags : new Object2ObjectOpenHashMap<>();
            this.blockTags = blockTags != null ? blockTags : new Object2ObjectOpenHashMap<>();
        }
    }
}

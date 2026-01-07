package com.gardenevery.vintagetag;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.annotation.Nullable;

import com.github.bsideup.jabel.Desugar;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
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

    public enum SyncType {
        NONE,
        FULL
    }

    public static void register() {
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("TagSync");
        MinecraftForge.EVENT_BUS.register(new EventHandler());

        NETWORK.registerMessage(
                (message, ctx) -> null,
                TagDataSyncMessage.class,
                0,
                Side.CLIENT
        );

        NETWORK.registerMessage(
                (message, ctx) -> null,
                ServerSyncMessage.class,
                1,
                Side.CLIENT
        );

        NETWORK.registerMessage(
                (message, ctx) -> null,
                ClientSyncMessage.class,
                2,
                Side.SERVER
        );
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
        var data = new TagData();

        data.itemTags = new Object2ObjectOpenHashMap<>();
        for (var tagName : TagManager.item().allTags()) {
            Set<ItemKey> keys = TagManager.item().getKeys(tagName);
            ObjectArrayList<ItemEntry> entries = new ObjectArrayList<>(keys.size());

            for (var key : keys) {
                var registryName = key.item().getRegistryName();
                if (registryName != null) {
                    entries.add(new ItemEntry(registryName.toString(), key.metadata()));
                }
            }
            data.itemTags.put(tagName, entries);
        }

        data.fluidTags = new Object2ObjectOpenHashMap<>();
        for (var tagName : TagManager.fluid().allTags()) {
            Set<Fluid> fluids = TagManager.fluid().getKeys(tagName);
            ObjectArrayList<String> fluidNames = new ObjectArrayList<>(fluids.size());

            for (var fluid : fluids) {
                var fluidName = FluidRegistry.getFluidName(fluid);
                if (fluidName != null) {
                    fluidNames.add(fluidName);
                }
            }
            data.fluidTags.put(tagName, fluidNames);
        }

        data.blockTags = new Object2ObjectOpenHashMap<>();
        for (var tagName : TagManager.block().allTags()) {
            Set<Block> blocks = TagManager.block().getKeys(tagName);
            ObjectArrayList<String> blockNames = new ObjectArrayList<>(blocks.size());

            for (var block : blocks) {
                var registryName = block.getRegistryName();
                if (registryName != null) {
                    blockNames.add(registryName.toString());
                }
            }
            data.blockTags.put(tagName, blockNames);
        }
        return data;
    }

    public static void syncOreDictionary(@Nullable EntityPlayerMP player) {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) {
            return;
        }

        var message = new ServerSyncMessage();

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

    public static class EventHandler {
        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP entityPlayerMP) {
                TagSync.sync(entityPlayerMP);

                if (TagConfig.enableSyncToOreDict) {
                    TagSync.syncOreDictionary(entityPlayerMP);
                }
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
            tagData = new TagData();

            int itemTagCount = buf.readInt();
            validateCount("itemTagCount", itemTagCount);
            tagData.itemTags = readItemTags(buf, itemTagCount);

            int fluidTagCount = buf.readInt();
            validateCount("fluidTagCount", fluidTagCount);
            tagData.fluidTags = readStringMap(buf, fluidTagCount);

            int blockTagCount = buf.readInt();
            validateCount("blockTagCount", blockTagCount);
            tagData.blockTags = readStringMap(buf, blockTagCount);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            var tempBuf = buf.alloc().buffer();
            try {
                tempBuf.writeByte(type.ordinal());

                tempBuf.writeInt(tagData.itemTags.size());
                writeItemTags(tempBuf, tagData.itemTags);

                tempBuf.writeInt(tagData.fluidTags.size());
                writeStringMap(tempBuf, tagData.fluidTags);

                tempBuf.writeInt(tagData.blockTags.size());
                writeStringMap(tempBuf, tagData.blockTags);

                int totalSize = tempBuf.readableBytes();
                validateSize(totalSize);

                buf.writeBytes(tempBuf);
            } finally {
                tempBuf.release();
            }
        }

        private void validateSize(int size) {
            if (size > MAX_PACKET_SIZE) {
                TagLog.error("Packet too large: {} bytes, max allowed: {}", size, MAX_PACKET_SIZE);
            }
        }

        private void validateCount(String fieldName, int count) {
            if (count < 0) {
                TagLog.error("Invalid {}: {}", fieldName, count);
            }
        }

        private SyncType readSyncType(int ordinal) {
            if (ordinal < 0 || ordinal >= SyncType.values().length) {
                return SyncType.NONE;
            }
            return SyncType.values()[ordinal];
        }

        private Object2ObjectMap<String, ObjectArrayList<ItemEntry>> readItemTags(ByteBuf buf, int tagCount) {
            Object2ObjectMap<String, ObjectArrayList<ItemEntry>> itemTags = new Object2ObjectOpenHashMap<>(tagCount);
            for (int i = 0; i < tagCount; i++) {
                var tagName = readString(buf);
                int entryCount = buf.readInt();
                validateCount("item entry count", entryCount);

                ObjectArrayList<ItemEntry> entries = new ObjectArrayList<>(entryCount);
                for (int j = 0; j < entryCount; j++) {
                    var itemId = readString(buf);
                    int metadata = buf.readInt();
                    entries.add(new ItemEntry(itemId, metadata));
                }
                itemTags.put(tagName, entries);
            }
            return itemTags;
        }

        private Object2ObjectMap<String, ObjectArrayList<String>> readStringMap(ByteBuf buf, int tagCount) {
            Object2ObjectMap<String, ObjectArrayList<String>> map = new Object2ObjectOpenHashMap<>(tagCount);
            for (int i = 0; i < tagCount; i++) {
                var tagName = readString(buf);
                int count = buf.readInt();
                validateCount("entry count", count);

                ObjectArrayList<String> entries = new ObjectArrayList<>(count);
                for (int j = 0; j < count; j++) {
                    entries.add(readString(buf));
                }
                map.put(tagName, entries);
            }
            return map;
        }

        private void writeItemTags(ByteBuf buf, Object2ObjectMap<String, ObjectArrayList<ItemEntry>> itemTags) {
            for (var entry : itemTags.object2ObjectEntrySet()) {
                writeString(buf, entry.getKey());
                buf.writeInt(entry.getValue().size());
                for (var itemEntry : entry.getValue()) {
                    writeString(buf, itemEntry.itemId);
                    buf.writeInt(itemEntry.metadata);
                }
            }
        }

        private void writeStringMap(ByteBuf buf, Object2ObjectMap<String, ObjectArrayList<String>> map) {
            for (var entry : map.object2ObjectEntrySet()) {
                writeString(buf, entry.getKey());
                buf.writeInt(entry.getValue().size());
                for (var value : entry.getValue()) {
                    writeString(buf, value);
                }
            }
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
    }

    public static class ServerSyncMessage implements IMessage {
        public ServerSyncMessage() {}

        @Override
        public void fromBytes(ByteBuf buf) {}

        @Override
        public void toBytes(ByteBuf buf) {}
    }

    public static class ClientSyncMessage implements IMessage {
        public ClientSyncMessage() {}

        @Override
        public void fromBytes(ByteBuf buf) {}

        @Override
        public void toBytes(ByteBuf buf) {}
    }

    public static class TagData {
        public Object2ObjectMap<String, ObjectArrayList<ItemEntry>> itemTags;
        public Object2ObjectMap<String, ObjectArrayList<String>> fluidTags;
        public Object2ObjectMap<String, ObjectArrayList<String>> blockTags;
    }

    @Desugar
    public record ItemEntry(String itemId, int metadata) {}
}

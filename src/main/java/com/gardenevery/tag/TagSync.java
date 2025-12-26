package com.gardenevery.tag;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import com.github.bsideup.jabel.Desugar;

import io.netty.buffer.ByteBuf;

import net.minecraft.block.Block;
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

public final class TagSync {

    public static SimpleNetworkWrapper NETWORK;

    private static final int MAX_TAG_COUNT = 50000;
    private static final int MAX_ENTRY_COUNT = 50000;
    private static final int MAX_STRING_LENGTH = 65536;

    public enum SyncType {
        NONE,
        INCREMENTAL,
        FULL
    }

    public static void register() {
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("TagSync");
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        NETWORK.registerMessage(
                (message, ctx) -> null,
                ClientSyncMessage.class,
                0,
                Side.CLIENT
        );
    }

    public static void sync() {
        sync(null);
    }

    public static void sync(@Nullable net.minecraft.entity.player.EntityPlayerMP player) {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) {
            return;
        }

        var tagData = collectTagData();
        var message = new ClientSyncMessage(SyncType.FULL, tagData);

        if (player == null) {
            for (net.minecraft.entity.player.EntityPlayerMP onlinePlayer : FMLCommonHandler.instance()
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

        data.itemTags = new HashMap<>();
        for (var tagName : TagManager.ITEM.getAllTag()) {
            Set<ItemKey> keys = TagManager.ITEM.getKey(tagName);
            List<ItemEntry> entries = new ArrayList<>();
            for (var key : keys) {
                var registryName = key.item().getRegistryName();
                if (registryName != null) {
                    entries.add(new ItemEntry(registryName.toString(), key.metadata()));
                }
            }
            data.itemTags.put(tagName, entries);
        }

        data.fluidTags = new HashMap<>();
        for (var tagName : TagManager.FLUID.getAllTag()) {
            Set<Fluid> fluids = TagManager.FLUID.getKey(tagName);
            List<String> fluidNames = new ArrayList<>();
            for (var fluid : fluids) {
                var fluidName = FluidRegistry.getFluidName(fluid);
                if (fluidName != null) {
                    fluidNames.add(fluidName);
                }
            }
            data.fluidTags.put(tagName, fluidNames);
        }

        data.blockTags = new HashMap<>();
        for (var tagName : TagManager.BLOCK.getAllTag()) {
            Set<Block> blocks = TagManager.BLOCK.getKey(tagName);
            List<String> blockNames = new ArrayList<>();
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

    public static class EventHandler {
        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                TagSync.sync((net.minecraft.entity.player.EntityPlayerMP) event.player);
            }
        }
    }

    public static class ClientSyncMessage implements IMessage {
        public SyncType syncType;
        public TagData tagData;

        public ClientSyncMessage() {}

        public ClientSyncMessage(SyncType syncType, TagData tagData) {
            this.syncType = syncType;
            this.tagData = tagData;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            int st = buf.readUnsignedByte();
            if (st < 0 || st >= SyncType.values().length) {
                syncType = SyncType.NONE;
            } else {
                syncType = SyncType.values()[st];
            }

            int itemTagCount = buf.readInt();
            if (itemTagCount < 0 || itemTagCount > MAX_TAG_COUNT) {
                throw new IllegalArgumentException("Invalid itemTagCount: " + itemTagCount);
            }
            tagData = new TagData();
            tagData.itemTags = new HashMap<>(itemTagCount);
            for (int i = 0; i < itemTagCount; i++) {
                var tagName = readString(buf);
                int entryCount = buf.readInt();
                if (entryCount < 0 || entryCount > MAX_ENTRY_COUNT) {
                    throw new IllegalArgumentException("Invalid item entry count: " + entryCount);
                }
                List<ItemEntry> entries = new ArrayList<>(entryCount);
                for (int j = 0; j < entryCount; j++) {
                    var itemId = readString(buf);
                    int metadata = buf.readInt();
                    entries.add(new ItemEntry(itemId, metadata));
                }
                tagData.itemTags.put(tagName, entries);
            }

            int fluidTagCount = buf.readInt();
            if (fluidTagCount < 0 || fluidTagCount > MAX_TAG_COUNT) {
                throw new IllegalArgumentException("Invalid fluidTagCount: " + fluidTagCount);
            }
            tagData.fluidTags = new HashMap<>(fluidTagCount);
            for (int i = 0; i < fluidTagCount; i++) {
                var tagName = readString(buf);
                int fluidCount = buf.readInt();
                if (fluidCount < 0 || fluidCount > MAX_ENTRY_COUNT) {
                    throw new IllegalArgumentException("Invalid fluid count: " + fluidCount);
                }
                List<String> fluids = new ArrayList<>(fluidCount);
                for (int j = 0; j < fluidCount; j++) {
                    fluids.add(readString(buf));
                }
                tagData.fluidTags.put(tagName, fluids);
            }

            int blockTagCount = buf.readInt();
            if (blockTagCount < 0 || blockTagCount > MAX_TAG_COUNT) {
                throw new IllegalArgumentException("Invalid blockTagCount: " + blockTagCount);
            }
            tagData.blockTags = new HashMap<>(blockTagCount);
            for (int i = 0; i < blockTagCount; i++) {
                var tagName = readString(buf);
                int blockCount = buf.readInt();
                if (blockCount < 0 || blockCount > MAX_ENTRY_COUNT) {
                    throw new IllegalArgumentException("Invalid block count: " + blockCount);
                }
                List<String> blocks = new ArrayList<>(blockCount);
                for (int j = 0; j < blockCount; j++) {
                    blocks.add(readString(buf));
                }
                tagData.blockTags.put(tagName, blocks);
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeByte(syncType.ordinal());

            buf.writeInt(tagData.itemTags.size());
            for (Map.Entry<String, List<ItemEntry>> entry : tagData.itemTags.entrySet()) {
                writeString(buf, entry.getKey());
                buf.writeInt(entry.getValue().size());
                for (var itemEntry : entry.getValue()) {
                    writeString(buf, itemEntry.itemId);
                    buf.writeInt(itemEntry.metadata);
                }
            }

            buf.writeInt(tagData.fluidTags.size());
            for (Map.Entry<String, List<String>> entry : tagData.fluidTags.entrySet()) {
                writeString(buf, entry.getKey());
                buf.writeInt(entry.getValue().size());
                for (var fluidName : entry.getValue()) {
                    writeString(buf, fluidName);
                }
            }

            buf.writeInt(tagData.blockTags.size());
            for (Map.Entry<String, List<String>> entry : tagData.blockTags.entrySet()) {
                writeString(buf, entry.getKey());
                buf.writeInt(entry.getValue().size());
                for (var blockName : entry.getValue()) {
                    writeString(buf, blockName);
                }
            }
        }

        private String readString(ByteBuf buf) {
            int length = buf.readInt();
            if (length < 0 || length > MAX_STRING_LENGTH) {
                throw new IllegalArgumentException("Invalid string length: " + length);
            }
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private void writeString(ByteBuf buf, String str) {
            if (str == null) {
                buf.writeInt(0);
                return;
            }
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }
    }

    public static class TagData {
        public Map<String, List<ItemEntry>> itemTags;
        public Map<String, List<String>> fluidTags;
        public Map<String, List<String>> blockTags;
    }

    @Desugar
    public record ItemEntry(String itemId, int metadata) {}
}

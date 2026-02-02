package com.gardenevery.vintagetag;

import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

import com.github.bsideup.jabel.Desugar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

final class NetworkSync {
	public static SimpleNetworkWrapper NETWORK;
	private static final int MAX_PACKET_SIZE = 2 * 1024 * 1024;
	private static final int INVALID_ID = -1;

	public enum SyncType {
		NONE, FULL
	}

	public static void register() {
		NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("VintageTag");
		NETWORK.registerMessage((message, ctx) -> null, TagDataSyncMessage.class, 0, Side.CLIENT);
	}

	public static void sync(@Nullable EntityPlayerMP player) {
		var server = FMLCommonHandler.instance().getMinecraftServerInstance();
		if (server == null) {
			return;
		}

		var tagData = collectTagData();
		var message = new TagDataSyncMessage(SyncType.FULL, tagData);

		if (player == null) {
			for (var onlinePlayer : server.getPlayerList().getPlayers()) {
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
		var allEntries = TagManager.item().getAllEntries();
		if (allEntries.isEmpty()) {
			return new Object2ObjectOpenHashMap<>();
		}

		var itemTags = new Object2ObjectOpenHashMap<String, IntArrayList>(allEntries.size());

		for (var entry : allEntries.entrySet()) {
			var tagName = entry.getKey();
			var keys = entry.getValue();

			if (!keys.isEmpty()) {
				var entries = new IntArrayList(keys.size() * 2);

				for (var key : keys) {
					int id = getItemId(key.item());
					if (id != INVALID_ID) {
						entries.add(id);
						entries.add(key.metadata());
					}
				}

				if (!entries.isEmpty()) {
					itemTags.put(tagName, entries);
				}
			}
		}

		return itemTags;
	}

	private static Object2ObjectMap<String, ObjectArrayList<String>> collectFluidTags() {
		var allEntries = TagManager.fluid().getAllEntries();
		if (allEntries.isEmpty()) {
			return new Object2ObjectOpenHashMap<>();
		}

		var fluidTags = new Object2ObjectOpenHashMap<String, ObjectArrayList<String>>(allEntries.size());

		for (var entry : allEntries.entrySet()) {
			var tagName = entry.getKey();
			var fluids = entry.getValue();

			if (!fluids.isEmpty()) {
				var fluidNames = new ObjectArrayList<String>(fluids.size());

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
		}

		return fluidTags;
	}

	private static Object2ObjectMap<String, IntArrayList> collectBlockTags() {
		var allEntries = TagManager.block().getAllEntries();
		if (allEntries.isEmpty()) {
			return new Object2ObjectOpenHashMap<>();
		}

		var blockTags = new Object2ObjectOpenHashMap<String, IntArrayList>(allEntries.size());

		for (var entry : allEntries.entrySet()) {
			var tagName = entry.getKey();
			var blocks = entry.getValue();

			if (!blocks.isEmpty()) {
				var blockIds = new IntArrayList(blocks.size());

				for (var block : blocks) {
					int id = getBlockId(block);
					if (id != INVALID_ID) {
						blockIds.add(id);
					}
				}

				if (!blockIds.isEmpty()) {
					blockTags.put(tagName, blockIds);
				}
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
				NetworkSync.sync(player);
			}
		}
	}

	public static class TagDataSyncMessage implements IMessage {

		public SyncType type;
		public TagData tagData;

		public TagDataSyncMessage() {
		}

		public TagDataSyncMessage(SyncType type, TagData tagData) {
			this.type = type;
			this.tagData = tagData;
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			validatePacketIntegrity(buf);

			int st = buf.readUnsignedByte();
			type = readSyncType(st);

			Object2ObjectMap<String, IntArrayList> itemTags = readOptionalItemTags(buf);
			Object2ObjectMap<String, ObjectArrayList<String>> fluidTags = readOptionalFluidTags(buf);
			Object2ObjectMap<String, IntArrayList> blockTags = readOptionalBlockTags(buf);

			tagData = new TagData(itemTags, fluidTags, blockTags);
		}

		@Override
		public void toBytes(ByteBuf buf) {
			var tempBuf = buf.alloc().buffer();

			try {
				tempBuf.writeByte(type.ordinal());
				writeOptionalItemTags(tempBuf, tagData.itemTags());
				writeOptionalFluidTags(tempBuf, tagData.fluidTags());
				writeOptionalBlockTags(tempBuf, tagData.blockTags());

				int totalSize = tempBuf.readableBytes();
				validateSize(totalSize);

				buf.writeBytes(tempBuf);
			} finally {
				tempBuf.release();
			}
		}

		private void validatePacketIntegrity(ByteBuf buf) {
			int totalSize = buf.readableBytes();
			validateSize(totalSize);

			if (totalSize < 1) {
				throw new IllegalArgumentException("Packet too small, missing sync type");
			}
		}

		private Object2ObjectMap<String, IntArrayList> readOptionalItemTags(ByteBuf buf) {
			if (!buf.readBoolean()) {
				return new Object2ObjectOpenHashMap<>(0);
			}

			int tagCount = buf.readInt();
			validateCount(tagCount);

			var map = new Object2ObjectOpenHashMap<String, IntArrayList>(tagCount);

			for (int i = 0; i < tagCount; i++) {
				var tagName = readStringSafe(buf);
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

		private Object2ObjectMap<String, ObjectArrayList<String>> readOptionalFluidTags(ByteBuf buf) {
			if (!buf.readBoolean()) {
				return new Object2ObjectOpenHashMap<>(0);
			}

			int tagCount = buf.readInt();
			validateCount(tagCount);

			var map = new Object2ObjectOpenHashMap<String, ObjectArrayList<String>>(tagCount);

			for (int i = 0; i < tagCount; i++) {
				var tagName = readStringSafe(buf);
				int entryCount = buf.readInt();
				validateCount(entryCount);

				var fluidNames = new ObjectArrayList<String>(entryCount);
				for (int j = 0; j < entryCount; j++) {
					var fluidName = readStringSafe(buf);
					fluidNames.add(fluidName);
				}

				map.put(tagName, fluidNames);
			}

			return map;
		}

		private Object2ObjectMap<String, IntArrayList> readOptionalBlockTags(ByteBuf buf) {
			if (!buf.readBoolean()) {
				return new Object2ObjectOpenHashMap<>(0);
			}

			int tagCount = buf.readInt();
			validateCount(tagCount);

			var map = new Object2ObjectOpenHashMap<String, IntArrayList>(tagCount);

			for (int i = 0; i < tagCount; i++) {
				var tagName = readStringSafe(buf);
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

		private void writeOptionalItemTags(ByteBuf buf, Object2ObjectMap<String, IntArrayList> map) {
			boolean hasMap = map != null && !map.isEmpty();
			buf.writeBoolean(hasMap);

			if (hasMap) {
				buf.writeInt(map.size());

				for (var entry : map.object2ObjectEntrySet()) {
					writeStringSafe(buf, entry.getKey());
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
		}

		private void writeOptionalFluidTags(ByteBuf buf, Object2ObjectMap<String, ObjectArrayList<String>> map) {
			boolean hasMap = map != null && !map.isEmpty();
			buf.writeBoolean(hasMap);

			if (hasMap) {
				buf.writeInt(map.size());

				for (var entry : map.object2ObjectEntrySet()) {
					writeStringSafe(buf, entry.getKey());
					var fluidNames = entry.getValue();
					buf.writeInt(fluidNames.size());

					for (var fluidName : fluidNames) {
						writeStringSafe(buf, fluidName);
					}
				}
			}
		}

		private void writeOptionalBlockTags(ByteBuf buf, Object2ObjectMap<String, IntArrayList> map) {
			boolean hasMap = map != null && !map.isEmpty();
			buf.writeBoolean(hasMap);

			if (hasMap) {
				buf.writeInt(map.size());

				for (var entry : map.object2ObjectEntrySet()) {
					writeStringSafe(buf, entry.getKey());
					var blockIds = entry.getValue();
					buf.writeInt(blockIds.size());

					for (int blockId : blockIds) {
						buf.writeInt(blockId);
					}
				}
			}
		}

		private void validateSize(int size) {
			if (size > MAX_PACKET_SIZE) {
				throw new IllegalArgumentException(
						String.format("Packet too large: %d bytes, max allowed: %d", size, MAX_PACKET_SIZE));
			}
		}

		private void validateCount(int count) {
			if (count < 0) {
				throw new IllegalArgumentException("Invalid count: " + count);
			}
		}

		private SyncType readSyncType(int ordinal) {
			if (ordinal < 0 || ordinal >= SyncType.values().length) {
				return SyncType.NONE;
			}

			return SyncType.values()[ordinal];
		}

		private String readStringSafe(ByteBuf buf) {
			int length = buf.readInt();

			if (length < 0) {
				throw new IllegalArgumentException("Negative string length: " + length);
			}

			if (length == 0) {
				return "";
			}

			if (buf.readableBytes() < length) {
				throw new IllegalArgumentException(
						String.format("Not enough bytes for string: need %d, have %d", length, buf.readableBytes()));
			}

			byte[] bytes = new byte[length];
			buf.readBytes(bytes);
			return new String(bytes, StandardCharsets.UTF_8);
		}

		private void writeStringSafe(ByteBuf buf, String string) {
			if (string == null || string.isEmpty()) {
				buf.writeInt(0);
				return;
			}

			int length = string.getBytes(StandardCharsets.UTF_8).length;
			buf.writeInt(length);
			ByteBufUtil.writeUtf8(buf, string);
		}
	}

	@Desugar
	public record TagData(Object2ObjectMap<String, IntArrayList> itemTags,
			Object2ObjectMap<String, ObjectArrayList<String>> fluidTags,
			Object2ObjectMap<String, IntArrayList> blockTags) {

		public TagData(Object2ObjectMap<String, IntArrayList> itemTags,
				Object2ObjectMap<String, ObjectArrayList<String>> fluidTags,
				Object2ObjectMap<String, IntArrayList> blockTags) {
			this.itemTags = itemTags != null ? itemTags : new Object2ObjectOpenHashMap<>(0);
			this.fluidTags = fluidTags != null ? fluidTags : new Object2ObjectOpenHashMap<>(0);
			this.blockTags = blockTags != null ? blockTags : new Object2ObjectOpenHashMap<>(0);
		}
	}
}

package com.gardenevery.vintagetag;

import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

import com.gardenevery.vintagetag.TagEntry.BlockEntry;
import com.gardenevery.vintagetag.TagEntry.FluidEntry;
import com.gardenevery.vintagetag.TagEntry.ItemEntry;
import com.github.bsideup.jabel.Desugar;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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
	private static final int TYPE_TAG = -1;

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

	private static Object2ObjectMap<String, ObjectArrayList<ItemEntry>> collectItemTags() {
		var allEntries = TagManager.item().getAllEntries();
		if (allEntries.isEmpty()) {
			return new Object2ObjectOpenHashMap<>();
		}

		var itemTags = new Object2ObjectOpenHashMap<String, ObjectArrayList<ItemEntry>>(allEntries.size());

		for (var entry : allEntries.entrySet()) {
			var tagName = entry.getKey();
			var keys = entry.getValue();

			if (!keys.isEmpty()) {
				var entries = new ObjectArrayList<ItemEntry>(keys.size());
				entries.addAll(keys);

				if (!entries.isEmpty()) {
					itemTags.put(tagName, entries);
				}
			}
		}

		return itemTags;
	}

	private static Object2ObjectMap<String, ObjectArrayList<FluidEntry>> collectFluidTags() {
		var allEntries = TagManager.fluid().getAllEntries();
		if (allEntries.isEmpty()) {
			return new Object2ObjectOpenHashMap<>();
		}

		var fluidTags = new Object2ObjectOpenHashMap<String, ObjectArrayList<FluidEntry>>(allEntries.size());

		for (var entry : allEntries.entrySet()) {
			var tagName = entry.getKey();
			var fluids = entry.getValue();

			if (!fluids.isEmpty()) {
				var fluidEntries = new ObjectArrayList<FluidEntry>(fluids.size());
				fluidEntries.addAll(fluids);

				if (!fluidEntries.isEmpty()) {
					fluidTags.put(tagName, fluidEntries);
				}
			}
		}

		return fluidTags;
	}

	private static Object2ObjectMap<String, ObjectArrayList<BlockEntry>> collectBlockTags() {
		var allEntries = TagManager.block().getAllEntries();
		if (allEntries.isEmpty()) {
			return new Object2ObjectOpenHashMap<>();
		}

		var blockTags = new Object2ObjectOpenHashMap<String, ObjectArrayList<BlockEntry>>(allEntries.size());

		for (var entry : allEntries.entrySet()) {
			var tagName = entry.getKey();
			var blocks = entry.getValue();

			if (!blocks.isEmpty()) {
				var blockEntries = new ObjectArrayList<BlockEntry>(blocks.size());
				blockEntries.addAll(blocks);

				if (!blockEntries.isEmpty()) {
					blockTags.put(tagName, blockEntries);
				}
			}
		}

		return blockTags;
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

			Object2ObjectMap<String, ObjectArrayList<ItemEntry>> itemTags = readItemTags(buf);
			Object2ObjectMap<String, ObjectArrayList<FluidEntry>> fluidTags = readFluidTags(buf);
			Object2ObjectMap<String, ObjectArrayList<BlockEntry>> blockTags = readBlockTags(buf);

			tagData = new TagData(itemTags, fluidTags, blockTags);
		}

		@Override
		public void toBytes(ByteBuf buf) {
			var tempBuf = buf.alloc().buffer();

			try {
				tempBuf.writeByte(type.ordinal());
				writeItemTags(tempBuf, tagData.itemTags());
				writeFluidTags(tempBuf, tagData.fluidTags());
				writeBlockTags(tempBuf, tagData.blockTags());

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

		private Object2ObjectMap<String, ObjectArrayList<ItemEntry>> readItemTags(ByteBuf buf) {
			if (!buf.readBoolean()) {
				return new Object2ObjectOpenHashMap<>(0);
			}

			int tagCount = buf.readInt();
			validateCount(tagCount);

			var map = new Object2ObjectOpenHashMap<String, ObjectArrayList<ItemEntry>>(tagCount);

			for (int i = 0; i < tagCount; i++) {
				var tagName = readStringSafe(buf);
				int entryCount = buf.readInt();
				validateCount(entryCount);

				var entries = new ObjectArrayList<ItemEntry>(entryCount);
				for (int j = 0; j < entryCount; j++) {
					var entry = readItemEntry(buf);
					if (entry != null) {
						entries.add(entry);
					}
				}

				map.put(tagName, entries);
			}

			return map;
		}

		private Object2ObjectMap<String, ObjectArrayList<FluidEntry>> readFluidTags(ByteBuf buf) {
			if (!buf.readBoolean()) {
				return new Object2ObjectOpenHashMap<>(0);
			}

			int tagCount = buf.readInt();
			validateCount(tagCount);

			var map = new Object2ObjectOpenHashMap<String, ObjectArrayList<FluidEntry>>(tagCount);

			for (int i = 0; i < tagCount; i++) {
				var tagName = readStringSafe(buf);
				int entryCount = buf.readInt();
				validateCount(entryCount);

				var entries = new ObjectArrayList<FluidEntry>(entryCount);
				for (int j = 0; j < entryCount; j++) {
					var entry = readFluidEntry(buf);
					if (entry != null) {
						entries.add(entry);
					}
				}

				map.put(tagName, entries);
			}

			return map;
		}

		private Object2ObjectMap<String, ObjectArrayList<BlockEntry>> readBlockTags(ByteBuf buf) {
			if (!buf.readBoolean()) {
				return new Object2ObjectOpenHashMap<>(0);
			}

			int tagCount = buf.readInt();
			validateCount(tagCount);

			var map = new Object2ObjectOpenHashMap<String, ObjectArrayList<BlockEntry>>(tagCount);

			for (int i = 0; i < tagCount; i++) {
				var tagName = readStringSafe(buf);
				int entryCount = buf.readInt();
				validateCount(entryCount);

				var entries = new ObjectArrayList<BlockEntry>(entryCount);
				for (int j = 0; j < entryCount; j++) {
					var entry = readBlockEntry(buf);
					if (entry != null) {
						entries.add(entry);
					}
				}

				map.put(tagName, entries);
			}

			return map;
		}

		private ItemEntry readItemEntry(ByteBuf buf) {
			int typeMarker = buf.readInt();

			if (typeMarker == TYPE_TAG) {
				var tagName = readStringSafe(buf);
				return TagEntry.item(tagName);
			} else {
				if (typeMarker < 0) {
					return null;
				}

				int metadata = buf.readInt();
				var item = Item.getItemById(typeMarker);
				return TagEntry.item(item, metadata);
			}
		}

		private FluidEntry readFluidEntry(ByteBuf buf) {
			int typeMarker = buf.readInt();

			if (typeMarker == TYPE_TAG) {
				var tagName = readStringSafe(buf);
				return TagEntry.fluid(tagName);
			} else {
				var fluidName = readStringSafe(buf);
				var fluid = FluidRegistry.getFluid(fluidName);
				if (fluid == null) {
					return null;
				}
				return TagEntry.fluid(fluid);
			}
		}

		private BlockEntry readBlockEntry(ByteBuf buf) {
			int typeMarker = buf.readInt();

			if (typeMarker == TYPE_TAG) {
				var tagName = readStringSafe(buf);
				return TagEntry.block(tagName);
			} else {
				if (typeMarker < 0) {
					return null;
				}

				var block = Block.getBlockById(typeMarker);
				return TagEntry.block(block);
			}
		}

		private void writeItemTags(ByteBuf buf, Object2ObjectMap<String, ObjectArrayList<ItemEntry>> map) {
			boolean hasMap = map != null && !map.isEmpty();
			buf.writeBoolean(hasMap);

			if (hasMap) {
				buf.writeInt(map.size());

				for (var entry : map.object2ObjectEntrySet()) {
					writeStringSafe(buf, entry.getKey());
					var itemEntries = entry.getValue();
					buf.writeInt(itemEntries.size());

					for (var itemEntry : itemEntries) {
						writeItemEntry(buf, itemEntry);
					}
				}
			}
		}

		private void writeFluidTags(ByteBuf buf, Object2ObjectMap<String, ObjectArrayList<FluidEntry>> map) {
			boolean hasMap = map != null && !map.isEmpty();
			buf.writeBoolean(hasMap);

			if (hasMap) {
				buf.writeInt(map.size());

				for (var entry : map.object2ObjectEntrySet()) {
					writeStringSafe(buf, entry.getKey());
					var fluidEntries = entry.getValue();
					buf.writeInt(fluidEntries.size());

					for (var fluidEntry : fluidEntries) {
						writeFluidEntry(buf, fluidEntry);
					}
				}
			}
		}

		private void writeBlockTags(ByteBuf buf, Object2ObjectMap<String, ObjectArrayList<BlockEntry>> map) {
			boolean hasMap = map != null && !map.isEmpty();
			buf.writeBoolean(hasMap);

			if (hasMap) {
				buf.writeInt(map.size());

				for (var entry : map.object2ObjectEntrySet()) {
					writeStringSafe(buf, entry.getKey());
					var blockEntries = entry.getValue();
					buf.writeInt(blockEntries.size());

					for (var blockEntry : blockEntries) {
						writeBlockEntry(buf, blockEntry);
					}
				}
			}
		}

		private void writeItemEntry(ByteBuf buf, ItemEntry entry) {
			if (entry.isTag()) {
				buf.writeInt(TYPE_TAG);
				writeStringSafe(buf, entry.getTagName());
			} else if (entry instanceof ItemEntry.ItemKey itemKey) {
				buf.writeInt(Item.getIdFromItem(itemKey.item()));
				buf.writeInt(itemKey.metadata());
			}
		}

		private void writeFluidEntry(ByteBuf buf, FluidEntry entry) {
			if (entry.isTag()) {
				buf.writeInt(TYPE_TAG);
				writeStringSafe(buf, entry.getTagName());
			} else if (entry instanceof FluidEntry.FluidKey fluidKey) {
				buf.writeInt(0);
				var fluidName = FluidRegistry.getFluidName(fluidKey.fluid());
				writeStringSafe(buf, fluidName != null ? fluidName : "");
			}
		}

		private void writeBlockEntry(ByteBuf buf, BlockEntry entry) {
			if (entry.isTag()) {
				buf.writeInt(TYPE_TAG);
				writeStringSafe(buf, entry.getTagName());
			} else if (entry instanceof BlockEntry.BlockKey blockKey) {
				buf.writeInt(Block.getIdFromBlock(blockKey.block()));
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
	public record TagData(Object2ObjectMap<String, ObjectArrayList<ItemEntry>> itemTags,
			Object2ObjectMap<String, ObjectArrayList<FluidEntry>> fluidTags,
			Object2ObjectMap<String, ObjectArrayList<BlockEntry>> blockTags) {
		public TagData(Object2ObjectMap<String, ObjectArrayList<ItemEntry>> itemTags,
				Object2ObjectMap<String, ObjectArrayList<FluidEntry>> fluidTags,
				Object2ObjectMap<String, ObjectArrayList<BlockEntry>> blockTags) {
			this.itemTags = itemTags != null ? itemTags : new Object2ObjectOpenHashMap<>(0);
			this.fluidTags = fluidTags != null ? fluidTags : new Object2ObjectOpenHashMap<>(0);
			this.blockTags = blockTags != null ? blockTags : new Object2ObjectOpenHashMap<>(0);
		}
	}
}

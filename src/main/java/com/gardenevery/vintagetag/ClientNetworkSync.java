package com.gardenevery.vintagetag;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class ClientNetworkSync {

	@SideOnly(Side.CLIENT)
	public static void register() {
		if (NetworkSync.NETWORK == null) {
			NetworkSync.NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("VintageTag");
		}
		NetworkSync.NETWORK.registerMessage(TagDataSyncHandler.class, NetworkSync.TagDataSyncMessage.class, 0,
				Side.CLIENT);
	}

	@SideOnly(Side.CLIENT)
	public static class TagDataSyncHandler implements IMessageHandler<NetworkSync.TagDataSyncMessage, IMessage> {

		@Override
		public IMessage onMessage(NetworkSync.TagDataSyncMessage message, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> processClientSync(message));
			return null;
		}

		private void processClientSync(NetworkSync.TagDataSyncMessage message) {
			if (message == null || message.tagData == null || message.type == null
					|| message.type == NetworkSync.SyncType.NONE) {
				return;
			}

			TagManager.clear();
			processAllTags(message);
			TagManager.bake();
		}

		private void processAllTags(NetworkSync.TagDataSyncMessage message) {
			for (var itemEntry : message.tagData.itemTags().object2ObjectEntrySet()) {
				var tagName = itemEntry.getKey();
				var entries = itemEntry.getValue();

				if (!entries.isEmpty()) {
					var keys = new ObjectOpenHashSet<>(entries);
					TagManager.registerItem(keys, tagName);
				}
			}

			for (var fluidEntry : message.tagData.fluidTags().object2ObjectEntrySet()) {
				var tagName = fluidEntry.getKey();
				var entries = fluidEntry.getValue();

				if (!entries.isEmpty()) {
					var keys = new ObjectOpenHashSet<>(entries);
					TagManager.registerFluid(keys, tagName);
				}
			}

			for (var blockEntry : message.tagData.blockTags().object2ObjectEntrySet()) {
				var tagName = blockEntry.getKey();
				var entries = blockEntry.getValue();

				if (!entries.isEmpty()) {
					var keys = new ObjectOpenHashSet<>(entries);
					TagManager.registerBlock(keys, tagName);
				}
			}
		}
	}
}

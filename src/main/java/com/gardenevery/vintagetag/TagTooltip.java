package com.gardenevery.vintagetag;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class TagTooltip {
	private static boolean cachedShiftState = false;
	private static long lastCheckTime = 0;
	private static final long CHECK_INTERVAL = 50;

	@SubscribeEvent
	public void onItemTooltip(ItemTooltipEvent event) {
		var stack = event.getItemStack();
		if (stack.isEmpty()) {
			return;
		}

		var tooltip = event.getToolTip();
		if (!getCachedShiftPressed()) {
			tooltip.add(TextFormatting.GRAY + I18n.format("tag.tooltip.hold_shift"));
			return;
		}

		addTagsToTooltip(stack, tooltip);
	}

	private static boolean getCachedShiftPressed() {
		long currentTime = System.currentTimeMillis();

		if (currentTime - lastCheckTime >= CHECK_INTERVAL) {
			updateShiftCache();
			lastCheckTime = currentTime;
		}

		return cachedShiftState;
	}

	private static void updateShiftCache() {
		if (Keyboard.isCreated()) {
			cachedShiftState = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
		} else {
			cachedShiftState = false;
		}
	}

	private static void addTagsToTooltip(ItemStack itemStack, List<String> tooltip) {
		var itemTags = TagHelper.item().tags(itemStack);

		Set<String> fluidTags = Collections.emptySet();
		if (TagConfig.showFluidTags) {
			fluidTags = getFluidTags(itemStack);
		}

		Set<String> blockTags = Collections.emptySet();
		if (TagConfig.showBlockTags) {
			blockTags = getBlockTags(itemStack);
		}

		boolean hasItemTags = !itemTags.isEmpty();
		boolean hasFluidTags = !fluidTags.isEmpty();
		boolean hasBlockTags = !blockTags.isEmpty();

		if (!hasItemTags && !hasFluidTags && !hasBlockTags) {
			tooltip.add(TextFormatting.GRAY + "No tags");
			return;
		}

		tooltip.add("Tags:");

		if (hasItemTags) {
			addSortedTags(tooltip, itemTags, TextFormatting.WHITE);
		}

		if (hasFluidTags) {
			addSortedTags(tooltip, fluidTags, TextFormatting.BLUE);
		}

		if (hasBlockTags) {
			addSortedTags(tooltip, blockTags, TextFormatting.YELLOW);
		}
	}

	private static Set<String> getFluidTags(ItemStack itemStack) {
		if (!itemStack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
			return Collections.emptySet();
		}

		var fluidHandler = itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

		if (fluidHandler == null) {
			return Collections.emptySet();
		}

		var fluidStack = fluidHandler.drain(Integer.MAX_VALUE, false);
		if (fluidStack == null) {
			return Collections.emptySet();
		}

		return TagHelper.fluid().tags(fluidStack);
	}

	private static Set<String> getBlockTags(ItemStack itemStack) {
		var block = Block.getBlockFromItem(itemStack.getItem());

		if (block == Blocks.AIR) {
			return Collections.emptySet();
		}

		return TagHelper.block().tags(block);
	}

	private static void addSortedTags(List<String> tooltip, Set<String> tags, TextFormatting color) {
		if (tags.isEmpty()) {
			return;
		}

		var tagArray = tags.toArray(new String[0]);
		Arrays.sort(tagArray);

		for (var tag : tagArray) {
			tooltip.add(color + "  " + tag);
		}
	}
}

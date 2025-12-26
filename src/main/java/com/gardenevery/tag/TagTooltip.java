package com.gardenevery.tag;

import java.util.ArrayList;
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

    private static long lastKeyboardCheck = 0;
    private static boolean shiftDown = false;
    private static final long KEY_CHECK_INTERVAL = 50;

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        updateShiftState();
        List<String> tooltip = event.getToolTip();

        if (!shiftDown) {
            tooltip.add(TextFormatting.GRAY + I18n.format("tag.tooltip.hold_shift"));
            return;
        }
        addTagsToTooltip(stack, tooltip);
    }

    private static void updateShiftState() {
        if (System.currentTimeMillis() - lastKeyboardCheck > KEY_CHECK_INTERVAL) {
            shiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            lastKeyboardCheck = System.currentTimeMillis();
        }
    }

    private static void addTagsToTooltip(ItemStack stack, List<String> tooltip) {
        Set<String> itemTags = TagHelper.tags(stack);

        Set<String> fluidTags = null;
        if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
            var fluidHandler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            if (fluidHandler != null) {
                var fluid = fluidHandler.drain(Integer.MAX_VALUE, false);
                if (fluid != null && fluid.amount > 0) {
                    fluidTags = TagHelper.tags(fluid);
                }
            }
        }

        Set<String> blockTags = null;
        var block = Block.getBlockFromItem(stack.getItem());
        if (block != Blocks.AIR) {
            blockTags = TagHelper.tags(block);
        }

        boolean hasItemTags = !itemTags.isEmpty();
        boolean hasFluidTags = fluidTags != null && !fluidTags.isEmpty();
        boolean hasBlockTags = blockTags != null && !blockTags.isEmpty();

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

    private static void addSortedTags(List<String> tooltip, Set<String> tags, TextFormatting color) {
        List<String> sortedTags = new ArrayList<>(tags);
        Collections.sort(sortedTags);
        for (var tag : sortedTags) {
            tooltip.add(color + "  " + tag);
        }
    }
}

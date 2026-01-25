package com.gardenevery.vintagetag;

import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
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
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        var tooltip = event.getToolTip();

        if (!isShiftPressed()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("tag.tooltip.hold_shift"));
            return;
        }
        addTagsToTooltip(stack, tooltip);
    }

    private static boolean isShiftPressed() {
        return Keyboard.isCreated() && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
    }

    private static void addTagsToTooltip(ItemStack itemStack, List<String> tooltip) {
        var itemTags = TagHelper.item().tags(itemStack);

        Set<String> fluidTags = null;
        if (TagConfig.showFluidTags && itemStack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
            var fluidHandler = itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            if (fluidHandler != null) {
                var fluidStack = fluidHandler.drain(Integer.MAX_VALUE, false);
                if (fluidStack != null && fluidStack.amount > 0) {
                    fluidTags = TagHelper.fluid().tags(fluidStack);
                }
            }
        }

        Set<String> blockTags = null;
        if (TagConfig.showBlockTags) {
            var block = Block.getBlockFromItem(itemStack.getItem());
            blockTags = TagHelper.block().tags(block);
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
        tags.stream()
                .sorted()
                .forEach(tag -> tooltip.add(color + "  " + tag));
    }
}

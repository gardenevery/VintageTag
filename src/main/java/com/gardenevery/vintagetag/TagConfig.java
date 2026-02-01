package com.gardenevery.vintagetag;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Tags.MOD_ID)
@Config.LangKey("tag.config.title")
public class TagConfig {

	@Config.LangKey("tag.config.ore_sync")
	public static boolean enableOreSync = true;

	@Config.LangKey("tag.config.enable_mod_scanner")
	public static boolean enableModScanner = true;

	@Config.LangKey("tag.config.enable_config_scanner")
	public static boolean enableConfigScanner = true;

	@Config.LangKey("tag.config.enable_tooltip")
	@Config.RequiresMcRestart
	public static boolean enableTooltip = true;

	@Config.LangKey("tag.config.show_fluid_tags")
	public static boolean showFluidTags = false;

	@Config.LangKey("tag.config.show_block_tags")
	public static boolean showBlockTags = false;

	@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
	private static class EventHandler {

		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if (event.getModID().equals(Tags.MOD_ID)) {
				ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
			}
		}
	}
}

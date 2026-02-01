package com.gardenevery.vintagetag;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class VintageTag {

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		NetworkSync.register();
		MinecraftForge.EVENT_BUS.register(new NetworkSync.EventHandler());

		if (event.getSide() == Side.CLIENT) {
			ClientNetworkSync.register();
		}
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		if (TagConfig.enableTooltip && event.getSide() == Side.CLIENT) {
			var tooltip = new TagTooltip();
			MinecraftForge.EVENT_BUS.register(tooltip);
		}
	}

	@Mod.EventHandler
	public void onFMLoadComplete(FMLLoadCompleteEvent event) {
		boolean hasTasks = false;

		if (TagConfig.enableOreSync) {
			OreDictSync.sync();
			hasTasks = true;
		}

		if (TagConfig.enableModScanner) {
			TagLoader.scanModTags();
			hasTasks = true;
		}

		if (TagConfig.enableConfigScanner) {
			TagLoader.scanConfigTags();
			hasTasks = true;
		}

		if (hasTasks) {
			TagManager.bake();
		}
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new TagCommand());
	}
}

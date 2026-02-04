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
		TagLoader.scanModTags();
		TagManager.saveModTags();
		TagManager.clear();

		if (TagConfig.enableTooltip && event.getSide() == Side.CLIENT) {
			var tooltip = new TagTooltip();
			MinecraftForge.EVENT_BUS.register(tooltip);
		}
	}

	@Mod.EventHandler
	public void onFMLoadComplete(FMLLoadCompleteEvent event) {
		boolean hasTasks;

		OreDictSync.sync();
		TagManager.saveOreTags();
		TagLoader.scanModTags();
		TagManager.saveOreAndModTags();
		TagManager.clear();

		int tasks = 0;
		if (TagConfig.enableOreSync)
			tasks |= 1;
		if (TagConfig.enableModScanner)
			tasks |= 2;
		if (TagConfig.enableConfigScanner)
			tasks |= 4;

		hasTasks = switch (tasks) {
			case 1 -> {
				TagManager.applyOreTags();
				yield true;
			}

			case 2 -> {
				TagManager.applyModTags();
				yield true;
			}

			case 3 -> {
				TagManager.applyOreAndModTags();
				yield true;
			}

			case 4 -> {
				TagLoader.scanConfigTags();
				yield true;
			}

			case 5 -> {
				TagManager.applyOreTags();
				TagLoader.scanConfigTags();
				yield true;
			}

			case 6 -> {
				TagManager.applyModTags();
				TagLoader.scanConfigTags();
				yield true;
			}

			case 7 -> {
				TagManager.applyOreAndModTags();
				TagLoader.scanConfigTags();
				yield true;
			}
			default -> false;

		};

		if (hasTasks) {
			TagManager.bake();
		}
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new TagCommand());
	}
}

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
        TagSync.register();
        if (event.getSide() == Side.CLIENT) {
            ClientTagSync.register();
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
        if (TagConfig.enableOreSync) {
            OreSync.sync();
        }

        if (TagConfig.enableModScanner) {
            TagLoader.scanModTags();
        }

        if (TagConfig.enableConfigScanner) {
            TagLoader.scanConfigTags();
        }
        TagManager.bake();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new TagCommand());
    }
}

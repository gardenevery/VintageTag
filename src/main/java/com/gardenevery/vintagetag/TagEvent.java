package com.gardenevery.vintagetag;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired when the tag system update is complete.
 * Indicates that tag data has been refreshed, and previously cached tag information may be outdated.
 * <p>
 * Listen to this event to update locally cached tag data.
 */
public class TagEvent extends Event {}
package com.gardenevery.vintagetag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class TagLog {
    private static final Logger LOGGER = LogManager.getLogger("VintageTag");

    public static void info(String message, Object... params) {
        LOGGER.info(message, params);
    }
}

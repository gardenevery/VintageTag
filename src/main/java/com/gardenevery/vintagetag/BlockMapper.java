package com.gardenevery.vintagetag;

import java.util.Set;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

public final class BlockMapper {

    public static final int[] ALL_METADATA = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final Object2ReferenceOpenHashMap<ResourceLocation, ObjectOpenHashSet<IBlockState>> nameToBlockState = new Object2ReferenceOpenHashMap<>();

    @Nullable
    public static Set<IBlockState> get(String name) {
        return name != null ? get(new ResourceLocation(name)) : null;
    }

    @Nullable
    public static Set<IBlockState> get(ResourceLocation name) {
        if (name == null) {
            return null;
        }

        var blockStates = nameToBlockState.get(name);
        return blockStates != null && !blockStates.isEmpty() ? blockStates : null;
    }

    @SuppressWarnings("deprecation")
    public static void register(String name, Block block, int... metadata) {
        if (name == null || block == null || metadata == null) {
            return;
        }

        var resourceLocation = new ResourceLocation(name);
        var set = nameToBlockState.computeIfAbsent(resourceLocation, k -> new ObjectOpenHashSet<>());

        for (int meta : metadata) {
            set.add(block.getStateFromMeta(meta));
        }
    }

    public static void register() {
        register("minecraft:air", Blocks.AIR, 0);

        register("minecraft:stone", Blocks.STONE, 0);
        register("minecraft:granite", Blocks.STONE, 1);
        register("minecraft:polished_granite", Blocks.STONE, 2);
        register("minecraft:diorite", Blocks.STONE, 3);
        register("minecraft:polished_diorite", Blocks.STONE, 4);
        register("minecraft:andesite", Blocks.STONE, 5);
        register("minecraft:polished_andesite", Blocks.STONE, 6);

        register("minecraft:grass_block", Blocks.GRASS,0);

        register("minecraft:dirt", Blocks.DIRT, 0);
        register("minecraft:coarse_dirt", Blocks.DIRT, 1);
        register("minecraft:podzol", Blocks.DIRT, 2);

        register("minecraft:cobblestone", Blocks.COBBLESTONE, 0);

        register("minecraft:oak_planks", Blocks.PLANKS, 0);
        register("minecraft:spruce_planks", Blocks.PLANKS, 1);
        register("minecraft:birch_planks", Blocks.PLANKS, 2);
        register("minecraft:jungle_planks", Blocks.PLANKS, 3);
        register("minecraft:acacia_planks", Blocks.PLANKS, 4);
        register("minecraft:dark_oak_planks", Blocks.PLANKS, 5);

        register("minecraft:oak_sapling", Blocks.SAPLING, 0, 8);
        register("minecraft:spruce_sapling", Blocks.SAPLING, 1, 9);
        register("minecraft:birch_sapling", Blocks.SAPLING, 2, 10);
        register("minecraft:jungle_sapling", Blocks.SAPLING, 3, 11);
        register("minecraft:acacia_sapling", Blocks.SAPLING, 4, 12);
        register("minecraft:dark_oak_sapling", Blocks.SAPLING, 5, 13);

        register("minecraft:bedrock", Blocks.BEDROCK, 0);

        register("minecraft:water", Blocks.WATER, ALL_METADATA);
        register("minecraft:water", Blocks.FLOWING_WATER, ALL_METADATA);
        register("minecraft:lava", Blocks.LAVA, ALL_METADATA);
        register("minecraft:lava", Blocks.FLOWING_LAVA, ALL_METADATA);

        register("minecraft:sand", Blocks.SAND, 0);
        register("minecraft:red_sand", Blocks.SAND, 1);

        register("minecraft:gravel", Blocks.GRAVEL, 0);

        register("minecraft:gold_ore", Blocks.GOLD_ORE, 0);
        register("minecraft:iron_ore", Blocks.IRON_ORE, 0);
        register("minecraft:coal_ore", Blocks.COAL_ORE, 0);

        register("minecraft:oak_log", Blocks.LOG, 0, 4, 8, 12);
        register("minecraft:spruce_log", Blocks.LOG, 1, 5, 9, 13);
        register("minecraft:birch_log", Blocks.LOG, 2, 6, 10, 14);
        register("minecraft:jungle_log", Blocks.LOG, 3, 7, 11, 15);

        register("minecraft:oak_leaves", Blocks.LEAVES, 0, 4, 8, 12);
        register("minecraft:spruce_leaves", Blocks.LEAVES, 1, 5, 9, 13);
        register("minecraft:birch_leaves", Blocks.LEAVES, 2, 6, 10, 14);
        register("minecraft:jungle_leaves", Blocks.LEAVES, 3, 7, 11, 15);

        register("minecraft:sponge", Blocks.SPONGE, 0);
        register("minecraft:wet_sponge", Blocks.SPONGE, 1);

        register("minecraft:glass", Blocks.GLASS, 0);
        register("minecraft:lapis_ore", Blocks.LAPIS_ORE, 0);
        register("minecraft:lapis_block", Blocks.LAPIS_BLOCK, 0);
        register("minecraft:dispenser", Blocks.DISPENSER, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);

        register("minecraft:sandstone", Blocks.SANDSTONE, 0);
        register("minecraft:chiseled_sandstone", Blocks.SANDSTONE, 1);
        register("minecraft:cut_sandstone", Blocks.SANDSTONE, 2);

        register("minecraft:note_block", Blocks.NOTEBLOCK, 0);
        register("minecraft:bed", Blocks.BED, 0, 1, 2, 3, 8, 9, 10, 11, 12, 13, 14, 15);
        register("minecraft:powered_rail", Blocks.GOLDEN_RAIL, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);
        register("minecraft:detector_rail", Blocks.DETECTOR_RAIL, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);
        register("minecraft:sticky_piston", Blocks.STICKY_PISTON, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);
        register("minecraft:cobweb", Blocks.WEB, 0);

        register("minecraft:dead_bush", Blocks.TALLGRASS, 0);
        register("minecraft:tall_grass", Blocks.TALLGRASS, 1);
        register("minecraft:fern", Blocks.TALLGRASS, 2);

        register("minecraft:dead_bush", Blocks.DEADBUSH, 0);
        register("minecraft:piston", Blocks.PISTON, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);
        register("minecraft:piston_head", Blocks.PISTON_HEAD, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);

        register("minecraft:white_wool", Blocks.WOOL, 0);
        register("minecraft:orange_wool", Blocks.WOOL, 1);
        register("minecraft:magenta_wool", Blocks.WOOL, 2);
        register("minecraft:light_blue_wool", Blocks.WOOL, 3);
        register("minecraft:yellow_wool", Blocks.WOOL, 4);
        register("minecraft:lime_wool", Blocks.WOOL, 5);
        register("minecraft:pink_wool", Blocks.WOOL, 6);
        register("minecraft:gray_wool", Blocks.WOOL, 7);
        register("minecraft:light_gray_wool", Blocks.WOOL, 8);
        register("minecraft:cyan_wool", Blocks.WOOL, 9);
        register("minecraft:purple_wool", Blocks.WOOL, 10);
        register("minecraft:blue_wool", Blocks.WOOL, 11);
        register("minecraft:brown_wool", Blocks.WOOL, 12);
        register("minecraft:green_wool", Blocks.WOOL, 13);
        register("minecraft:red_wool", Blocks.WOOL, 14);
        register("minecraft:black_wool", Blocks.WOOL, 15);

        register("minecraft:moving_piston", Blocks.PISTON_EXTENSION, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);
        register("minecraft:dandelion", Blocks.YELLOW_FLOWER, 0);

        register("minecraft:poppy", Blocks.RED_FLOWER, 0);
        register("minecraft:blue_orchid", Blocks.RED_FLOWER, 1);
        register("minecraft:allium", Blocks.RED_FLOWER, 2);
        register("minecraft:azure_bluet", Blocks.RED_FLOWER, 3);
        register("minecraft:red_tulip", Blocks.RED_FLOWER, 4);
        register("minecraft:orange_tulip", Blocks.RED_FLOWER, 5);
        register("minecraft:white_tulip", Blocks.RED_FLOWER, 6);
        register("minecraft:pink_tulip", Blocks.RED_FLOWER, 7);
        register("minecraft:oxeye_daisy", Blocks.RED_FLOWER, 8);

        register("minecraft:brown_mushroom", Blocks.BROWN_MUSHROOM, 0);
        register("minecraft:red_mushroom", Blocks.RED_MUSHROOM, 0);
        register("minecraft:gold_block", Blocks.GOLD_BLOCK, 0);
        register("minecraft:iron_block", Blocks.IRON_BLOCK, 0);

        register("minecraft:stone_slab", Blocks.STONE_SLAB, 0, 8);
        register("minecraft:sandstone_slab", Blocks.STONE_SLAB, 1, 9);
        register("minecraft:petrified_oak_slab", Blocks.STONE_SLAB, 2, 10);
        register("minecraft:cobblestone_slab", Blocks.STONE_SLAB, 3, 11);
        register("minecraft:brick_slab", Blocks.STONE_SLAB, 4, 12);
        register("minecraft:stone_brick_slab", Blocks.STONE_SLAB, 5, 13);
        register("minecraft:nether_brick_slab", Blocks.STONE_SLAB, 6, 14);
        register("minecraft:quartz_slab", Blocks.STONE_SLAB, 7, 15);

        register("minecraft:smooth_stone", Blocks.DOUBLE_STONE_SLAB, 0, 8);
        register("minecraft:smooth_sandstone", Blocks.DOUBLE_STONE_SLAB, 1, 9);
        register("minecraft:petrified_oak_slab", Blocks.DOUBLE_STONE_SLAB, 2, 10);
        register("minecraft:cobblestone", Blocks.DOUBLE_STONE_SLAB, 3, 11);
        register("minecraft:bricks", Blocks.DOUBLE_STONE_SLAB, 4, 12);
        register("minecraft:stone_bricks", Blocks.DOUBLE_STONE_SLAB, 5, 13);
        register("minecraft:nether_bricks", Blocks.DOUBLE_STONE_SLAB, 6, 14);
        register("minecraft:smooth_quartz", Blocks.DOUBLE_STONE_SLAB, 7, 15);

        register("minecraft:bricks", Blocks.BRICK_BLOCK, 0);
        register("minecraft:tnt", Blocks.TNT, 0, 1);
        register("minecraft:bookshelf", Blocks.BOOKSHELF, 0);
        register("minecraft:mossy_cobblestone", Blocks.MOSSY_COBBLESTONE, 0);
        register("minecraft:obsidian", Blocks.OBSIDIAN, 0);

        register("minecraft:torch", Blocks.TORCH, 0, 5);
        register("minecraft:wall_torch", Blocks.TORCH, 1, 2, 3, 4);

        register("minecraft:fire", Blocks.FIRE, ALL_METADATA);
        register("minecraft:spawner", Blocks.MOB_SPAWNER, 0);
        register("minecraft:oak_stairs", Blocks.OAK_STAIRS, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:chest", Blocks.CHEST, 2, 3, 4, 5);
        register("minecraft:redstone_wire", Blocks.REDSTONE_WIRE, ALL_METADATA);
        register("minecraft:diamond_ore", Blocks.DIAMOND_ORE, 0);
        register("minecraft:diamond_block", Blocks.DIAMOND_BLOCK, 0);
        register("minecraft:crafting_table", Blocks.CRAFTING_TABLE, 0);
        register("minecraft:wheat", Blocks.WHEAT, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:farmland", Blocks.FARMLAND, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:furnace", Blocks.FURNACE, 2, 3, 4, 5);
        register("minecraft:furnace", Blocks.LIT_FURNACE, 2, 3, 4, 5);
        register("minecraft:sign", Blocks.STANDING_SIGN, ALL_METADATA);
        register("minecraft:oak_door", Blocks.OAK_DOOR, ALL_METADATA);
        register("minecraft:ladder", Blocks.LADDER, 2, 3, 4, 5);
        register("minecraft:rail", Blocks.RAIL, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        register("minecraft:cobblestone_stairs", Blocks.STONE_STAIRS, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:wall_sign", Blocks.WALL_SIGN, 2, 3, 4, 5);
        register("minecraft:lever", Blocks.LEVER, ALL_METADATA);
        register("minecraft:stone_pressure_plate", Blocks.STONE_PRESSURE_PLATE, 0, 1);
        register("minecraft:iron_door", Blocks.IRON_DOOR, ALL_METADATA);
        register("minecraft:oak_pressure_plate", Blocks.WOODEN_PRESSURE_PLATE, 0, 1);
        register("minecraft:redstone_ore", Blocks.REDSTONE_ORE, 2, 3, 4, 5);
        register("minecraft:redstone_ore", Blocks.LIT_REDSTONE_ORE, 2, 3, 4, 5);
        register("minecraft:redstone_wall_torch", Blocks.UNLIT_REDSTONE_TORCH, 1, 2, 3, 4);
        register("minecraft:redstone_torch", Blocks.UNLIT_REDSTONE_TORCH, 5);
        register("minecraft:redstone_wall_torch", Blocks.REDSTONE_TORCH, 1, 2, 3, 4);
        register("minecraft:redstone_torch", Blocks.REDSTONE_TORCH, 5);
        register("minecraft:stone_button", Blocks.STONE_BUTTON, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);
        register("minecraft:snow", Blocks.SNOW_LAYER, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:ice", Blocks.ICE, 0);
        register("minecraft:snow_block", Blocks.SNOW, 0);
        register("minecraft:cactus", Blocks.CACTUS, ALL_METADATA);
        register("minecraft:clay", Blocks.CLAY, 0);
        register("minecraft:sugar_cane", Blocks.REEDS, ALL_METADATA);
        register("minecraft:jukebox", Blocks.JUKEBOX, 0, 1);
        register("minecraft:oak_fence", Blocks.OAK_FENCE, 0);
        register("minecraft:carved_pumpkin", Blocks.PUMPKIN, 0, 1, 2, 3);
        register("minecraft:netherrack", Blocks.NETHERRACK, 0);
        register("minecraft:soul_sand", Blocks.SOUL_SAND, 0);
        register("minecraft:glowstone", Blocks.GLOWSTONE, 0);
        register("minecraft:nether_portal", Blocks.PORTAL, 0, 1, 2);
        register("minecraft:jack_o_lantern", Blocks.LIT_PUMPKIN, 0, 1, 2, 3);
        register("minecraft:cake", Blocks.CAKE, 0, 1, 2, 3, 4, 5, 6);
        register("minecraft:repeater", Blocks.UNPOWERED_REPEATER, ALL_METADATA);
        register("minecraft:repeater", Blocks.POWERED_REPEATER, ALL_METADATA);

        register("minecraft:white_stained_glass", Blocks.STAINED_GLASS, 0);
        register("minecraft:orange_stained_glass", Blocks.STAINED_GLASS, 1);
        register("minecraft:magenta_stained_glass", Blocks.STAINED_GLASS, 2);
        register("minecraft:light_blue_stained_glass", Blocks.STAINED_GLASS, 3);
        register("minecraft:yellow_stained_glass", Blocks.STAINED_GLASS, 4);
        register("minecraft:lime_stained_glass", Blocks.STAINED_GLASS, 5);
        register("minecraft:pink_stained_glass", Blocks.STAINED_GLASS, 6);
        register("minecraft:gray_stained_glass", Blocks.STAINED_GLASS, 7);
        register("minecraft:light_gray_stained_glass", Blocks.STAINED_GLASS, 8);
        register("minecraft:cyan_stained_glass", Blocks.STAINED_GLASS, 9);
        register("minecraft:purple_stained_glass", Blocks.STAINED_GLASS, 10);
        register("minecraft:blue_stained_glass", Blocks.STAINED_GLASS, 11);
        register("minecraft:brown_stained_glass", Blocks.STAINED_GLASS, 12);
        register("minecraft:green_stained_glass", Blocks.STAINED_GLASS, 13);
        register("minecraft:red_stained_glass", Blocks.STAINED_GLASS, 14);
        register("minecraft:black_stained_glass", Blocks.STAINED_GLASS, 15);

        register("minecraft:oak_trapdoor", Blocks.TRAPDOOR, ALL_METADATA);

        register("minecraft:infested_stone", Blocks.MONSTER_EGG, 0);
        register("minecraft:infested_cobblestone", Blocks.MONSTER_EGG, 1);
        register("minecraft:infested_stone_bricks", Blocks.MONSTER_EGG, 2);
        register("minecraft:infested_mossy_stone_bricks", Blocks.MONSTER_EGG, 3);
        register("minecraft:infested_cracked_stone_bricks", Blocks.MONSTER_EGG, 4);
        register("minecraft:infested_chiseled_stone_bricks", Blocks.MONSTER_EGG, 5);

        register("minecraft:stone_bricks", Blocks.STONEBRICK, 0);
        register("minecraft:mossy_stone_bricks", Blocks.STONEBRICK, 1);
        register("minecraft:cracked_stone_bricks", Blocks.STONEBRICK, 2);
        register("minecraft:chiseled_stone_bricks", Blocks.STONEBRICK, 3);

        register("minecraft:brown_mushroom_block", Blocks.BROWN_MUSHROOM_BLOCK,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15);
        register("minecraft:red_mushroom_block", Blocks.RED_MUSHROOM_BLOCK,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15);
        register("minecraft:mushroom_stem", Blocks.BROWN_MUSHROOM_BLOCK, 10);
        register("minecraft:mushroom_stem", Blocks.RED_MUSHROOM_BLOCK, 10);

        register("minecraft:iron_bars", Blocks.IRON_BARS, 0);
        register("minecraft:glass_pane", Blocks.GLASS_PANE, 0);
        register("minecraft:melon", Blocks.MELON_BLOCK, 0);
        register("minecraft:pumpkin_stem", Blocks.PUMPKIN_STEM, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:melon_stem", Blocks.MELON_STEM, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:vine", Blocks.VINE, ALL_METADATA);
        register("minecraft:oak_fence_gate", Blocks.OAK_FENCE_GATE, ALL_METADATA);
        register("minecraft:brick_stairs", Blocks.BRICK_STAIRS, 2, 3, 4, 5, 6, 7, 8, 9);
        register("minecraft:stone_brick_stairs", Blocks.STONE_BRICK_STAIRS, 2, 3, 4, 5, 6, 7, 8, 9);
        register("minecraft:mycelium", Blocks.MYCELIUM, 0);
        register("minecraft:lily_pad", Blocks.WATERLILY, 0);
        register("minecraft:nether_bricks", Blocks.NETHER_BRICK, 0);
        register("minecraft:nether_brick_fence", Blocks.NETHER_BRICK_FENCE, 0);
        register("minecraft:nether_brick_stairs", Blocks.NETHER_BRICK_STAIRS, 2, 3, 4, 5, 6, 7, 8, 9);
        register("minecraft:nether_wart", Blocks.NETHER_WART, 0, 1, 2, 3);
        register("minecraft:enchanting_table", Blocks.ENCHANTING_TABLE, 0);
        register("minecraft:brewing_stand", Blocks.BREWING_STAND, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:cauldron", Blocks.CAULDRON, 0, 1, 2, 3);
        register("minecraft:end_portal", Blocks.END_PORTAL, 0);
        register("minecraft:end_portal_frame", Blocks.END_PORTAL_FRAME, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:end_stone", Blocks.END_STONE, 0);
        register("minecraft:dragon_egg", Blocks.DRAGON_EGG, 0);
        register("minecraft:redstone_lamp", Blocks.REDSTONE_LAMP, 0);
        register("minecraft:redstone_lamp", Blocks.LIT_REDSTONE_LAMP, 0);

        register("minecraft:oak_slab", Blocks.WOODEN_SLAB, 0, 8);
        register("minecraft:spruce_slab", Blocks.WOODEN_SLAB, 1, 9);
        register("minecraft:birch_slab", Blocks.WOODEN_SLAB, 2, 10);
        register("minecraft:jungle_slab", Blocks.WOODEN_SLAB, 3, 11);
        register("minecraft:acacia_slab", Blocks.WOODEN_SLAB, 4, 12);
        register("minecraft:dark_oak_slab", Blocks.WOODEN_SLAB, 5, 13);

        register("minecraft:oak_planks", Blocks.DOUBLE_WOODEN_SLAB, 0);
        register("minecraft:spruce_planks", Blocks.DOUBLE_WOODEN_SLAB, 1);
        register("minecraft:birch_planks", Blocks.DOUBLE_WOODEN_SLAB, 2);
        register("minecraft:jungle_planks", Blocks.DOUBLE_WOODEN_SLAB, 3);
        register("minecraft:acacia_planks", Blocks.DOUBLE_WOODEN_SLAB, 4);
        register("minecraft:dark_oak_planks", Blocks.DOUBLE_WOODEN_SLAB, 5);

        register("minecraft:cocoa", Blocks.COCOA, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        register("minecraft:sandstone_stairs", Blocks.SANDSTONE_STAIRS, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:emerald_ore", Blocks.EMERALD_ORE, 0);
        register("minecraft:ender_chest", Blocks.ENDER_CHEST, 2, 3, 4, 5);
        register("minecraft:tripwire_hook", Blocks.TRIPWIRE_HOOK, ALL_METADATA);
        register("minecraft:tripwire", Blocks.TRIPWIRE, ALL_METADATA);
        register("minecraft:emerald_block", Blocks.EMERALD_BLOCK, 0);
        register("minecraft:spruce_stairs", Blocks.SPRUCE_STAIRS, ALL_METADATA);
        register("minecraft:birch_stairs", Blocks.BIRCH_STAIRS, ALL_METADATA);
        register("minecraft:jungle_stairs", Blocks.JUNGLE_STAIRS, ALL_METADATA);
        register("minecraft:command_block", Blocks.COMMAND_BLOCK, 2, 3, 4, 5, 10, 11, 12, 13);
        register("minecraft:beacon", Blocks.BEACON, 0);

        register("minecraft:cobblestone_wall", Blocks.COBBLESTONE_WALL, 0);
        register("minecraft:mossy_cobblestone_wall", Blocks.COBBLESTONE_WALL, 1);

        register("minecraft:flower_pot", Blocks.FLOWER_POT, ALL_METADATA);
        register("minecraft:carrots", Blocks.CARROTS, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:potatoes", Blocks.POTATOES, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:oak_button", Blocks.WOODEN_BUTTON, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);
        register("minecraft:skull", Blocks.SKULL, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);

        register("minecraft:anvil", Blocks.ANVIL, 0);
        register("minecraft:chipped_anvil", Blocks.ANVIL, 1);
        register("minecraft:damaged_anvil", Blocks.ANVIL, 2);

        register("minecraft:trapped_chest", Blocks.TRAPPED_CHEST, 2, 3, 4, 5);
        register("minecraft:light_weighted_pressure_plate", Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, ALL_METADATA);
        register("minecraft:heavy_weighted_pressure_plate", Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, ALL_METADATA);
        register("minecraft:comparator", Blocks.UNPOWERED_COMPARATOR, ALL_METADATA);
        register("minecraft:comparator", Blocks.POWERED_COMPARATOR, ALL_METADATA);
        register("minecraft:daylight_detector", Blocks.DAYLIGHT_DETECTOR, ALL_METADATA);
        register("minecraft:daylight_detector", Blocks.DAYLIGHT_DETECTOR_INVERTED, ALL_METADATA);
        register("minecraft:redstone_block", Blocks.REDSTONE_BLOCK, 0);
        register("minecraft:nether_quartz_ore", Blocks.QUARTZ_ORE, 0);
        register("minecraft:hopper", Blocks.HOPPER, 0, 2, 3, 4, 5, 8, 10, 11, 12, 13);

        register("minecraft:quartz_block", Blocks.QUARTZ_BLOCK, 0);
        register("minecraft:chiseled_quartz_block", Blocks.QUARTZ_BLOCK, 1);
        register("minecraft:quartz_pillar", Blocks.QUARTZ_BLOCK, 2, 3, 4);

        register("minecraft:quartz_stairs", Blocks.QUARTZ_STAIRS, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:activator_rail", Blocks.ACTIVATOR_RAIL, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);
        register("minecraft:dropper", Blocks.DROPPER, ALL_METADATA);

        register("minecraft:white_terracotta", Blocks.STAINED_HARDENED_CLAY, 0);
        register("minecraft:orange_terracotta", Blocks.STAINED_HARDENED_CLAY, 1);
        register("minecraft:magenta_terracotta", Blocks.STAINED_HARDENED_CLAY, 2);
        register("minecraft:light_blue_terracotta", Blocks.STAINED_HARDENED_CLAY, 3);
        register("minecraft:yellow_terracotta", Blocks.STAINED_HARDENED_CLAY, 4);
        register("minecraft:lime_terracotta", Blocks.STAINED_HARDENED_CLAY, 5);
        register("minecraft:pink_terracotta", Blocks.STAINED_HARDENED_CLAY, 6);
        register("minecraft:gray_terracotta", Blocks.STAINED_HARDENED_CLAY, 7);
        register("minecraft:light_gray_terracotta", Blocks.STAINED_HARDENED_CLAY, 8);
        register("minecraft:cyan_terracotta", Blocks.STAINED_HARDENED_CLAY, 9);
        register("minecraft:purple_terracotta", Blocks.STAINED_HARDENED_CLAY, 10);
        register("minecraft:blue_terracotta", Blocks.STAINED_HARDENED_CLAY, 11);
        register("minecraft:brown_terracotta", Blocks.STAINED_HARDENED_CLAY, 12);
        register("minecraft:green_terracotta", Blocks.STAINED_HARDENED_CLAY, 13);
        register("minecraft:red_terracotta", Blocks.STAINED_HARDENED_CLAY, 14);
        register("minecraft:black_terracotta", Blocks.STAINED_HARDENED_CLAY, 15);

        register("minecraft:white_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 0);
        register("minecraft:orange_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 1);
        register("minecraft:magenta_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 2);
        register("minecraft:light_blue_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 3);
        register("minecraft:yellow_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 4);
        register("minecraft:lime_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 5);
        register("minecraft:pink_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 6);
        register("minecraft:gray_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 7);
        register("minecraft:light_gray_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 8);
        register("minecraft:cyan_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 9);
        register("minecraft:purple_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 10);
        register("minecraft:blue_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 11);
        register("minecraft:brown_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 12);
        register("minecraft:green_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 13);
        register("minecraft:red_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 14);
        register("minecraft:black_stained_glass_pane", Blocks.STAINED_GLASS_PANE, 15);

        register("minecraft:acacia_leaves", Blocks.LEAVES2, 0, 4, 8, 12);
        register("minecraft:dark_oak_leaves", Blocks.LEAVES2, 1, 5, 9, 13);

        register("minecraft:acacia_log", Blocks.LOG2, 0, 4, 8, 12);
        register("minecraft:dark_oak_log", Blocks.LOG2, 1, 5, 9, 13);

        register("minecraft:acacia_stairs", Blocks.ACACIA_STAIRS, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:dark_oak_stairs", Blocks.DARK_OAK_STAIRS, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:slime_block", Blocks.SLIME_BLOCK, 0);
        register("minecraft:barrier", Blocks.BARRIER, 0);
        register("minecraft:iron_trapdoor", Blocks.IRON_TRAPDOOR, ALL_METADATA);

        register("minecraft:prismarine", Blocks.PRISMARINE, 0);
        register("minecraft:prismarine_bricks", Blocks.PRISMARINE, 1);
        register("minecraft:dark_prismarine", Blocks.PRISMARINE, 2);

        register("minecraft:sea_lantern", Blocks.SEA_LANTERN, 0);
        register("minecraft:hay_block", Blocks.HAY_BLOCK, 0, 4, 8);

        register("minecraft:white_carpet", Blocks.CARPET, 0);
        register("minecraft:orange_carpet", Blocks.CARPET, 1);
        register("minecraft:magenta_carpet", Blocks.CARPET, 2);
        register("minecraft:light_blue_carpet", Blocks.CARPET, 3);
        register("minecraft:yellow_carpet", Blocks.CARPET, 4);
        register("minecraft:lime_carpet", Blocks.CARPET, 5);
        register("minecraft:pink_carpet", Blocks.CARPET, 6);
        register("minecraft:gray_carpet", Blocks.CARPET, 7);
        register("minecraft:light_gray_carpet", Blocks.CARPET, 8);
        register("minecraft:cyan_carpet", Blocks.CARPET, 9);
        register("minecraft:purple_carpet", Blocks.CARPET, 10);
        register("minecraft:blue_carpet", Blocks.CARPET, 11);
        register("minecraft:brown_carpet", Blocks.CARPET, 12);
        register("minecraft:green_carpet", Blocks.CARPET, 13);
        register("minecraft:red_carpet", Blocks.CARPET, 14);
        register("minecraft:black_carpet", Blocks.CARPET, 15);

        register("minecraft:terracotta", Blocks.HARDENED_CLAY, 0);
        register("minecraft:coal_block", Blocks.COAL_BLOCK, 0);
        register("minecraft:packed_ice", Blocks.PACKED_ICE, 0);

        register("minecraft:sunflower", Blocks.DOUBLE_PLANT, 0);
        register("minecraft:lilac", Blocks.DOUBLE_PLANT, 1);
        register("minecraft:tall_grass", Blocks.DOUBLE_PLANT, 2);
        register("minecraft:large_fern", Blocks.DOUBLE_PLANT, 3);
        register("minecraft:rose_bush", Blocks.DOUBLE_PLANT, 4);
        register("minecraft:peony", Blocks.DOUBLE_PLANT, 5);

        register("minecraft:standing_banner", Blocks.STANDING_BANNER, ALL_METADATA);
        register("minecraft:wall_banner", Blocks.WALL_BANNER, 2, 3, 4, 5);

        register("minecraft:red_sandstone", Blocks.RED_SANDSTONE, 0);
        register("minecraft:chiseled_red_sandstone", Blocks.RED_SANDSTONE, 1);
        register("minecraft:cut_red_sandstone", Blocks.RED_SANDSTONE, 2);

        register("minecraft:red_sandstone_stairs", Blocks.RED_SANDSTONE_STAIRS, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:red_sandstone_slab", Blocks.STONE_SLAB2, 0, 8);
        register("minecraft:smooth_red_sandstone", Blocks.DOUBLE_STONE_SLAB2, 0, 8);

        register("minecraft:spruce_fence_gate", Blocks.SPRUCE_FENCE_GATE, ALL_METADATA);
        register("minecraft:birch_fence_gate", Blocks.BIRCH_FENCE_GATE, ALL_METADATA);
        register("minecraft:jungle_fence_gate", Blocks.JUNGLE_FENCE_GATE, ALL_METADATA);
        register("minecraft:dark_oak_fence_gate", Blocks.DARK_OAK_FENCE_GATE, ALL_METADATA);
        register("minecraft:acacia_fence_gate", Blocks.ACACIA_FENCE_GATE, ALL_METADATA);

        register("minecraft:spruce_fence", Blocks.SPRUCE_FENCE, 0);
        register("minecraft:birch_fence", Blocks.BIRCH_FENCE, 0);
        register("minecraft:jungle_fence", Blocks.JUNGLE_FENCE, 0);
        register("minecraft:dark_oak_fence", Blocks.DARK_OAK_FENCE, 0);
        register("minecraft:acacia_fence", Blocks.ACACIA_FENCE, 0);

        register("minecraft:spruce_door", Blocks.SPRUCE_DOOR, ALL_METADATA);
        register("minecraft:birch_door", Blocks.BIRCH_DOOR, ALL_METADATA);
        register("minecraft:jungle_door", Blocks.JUNGLE_DOOR, ALL_METADATA);
        register("minecraft:acacia_door", Blocks.ACACIA_DOOR, ALL_METADATA);
        register("minecraft:dark_oak_door", Blocks.DARK_OAK_DOOR, ALL_METADATA);

        register("minecraft:end_rod", Blocks.END_ROD, 0, 1, 2, 3, 4, 5);
        register("minecraft:chorus_plant", Blocks.CHORUS_PLANT, 0);
        register("minecraft:chorus_flower", Blocks.CHORUS_FLOWER, 0, 1, 2, 3, 4, 5);

        register("minecraft:purpur_block", Blocks.PURPUR_BLOCK, 0);
        register("minecraft:purpur_pillar", Blocks.PURPUR_PILLAR, 0, 4, 8);
        register("minecraft:purpur_stairs", Blocks.PURPUR_STAIRS, 0, 1, 2, 3, 4, 5, 6, 7);
        register("minecraft:purpur_slab", Blocks.PURPUR_SLAB, 0, 8);
        register("minecraft:purpur_slab", Blocks.PURPUR_DOUBLE_SLAB, 0);

        register("minecraft:end_stone_bricks", Blocks.END_BRICKS, 0);
        register("minecraft:beetroots", Blocks.BEETROOTS, 0, 1, 2, 3);
        register("minecraft:grass_path", Blocks.GRASS_PATH, 0);
        register("minecraft:end_gateway", Blocks.END_GATEWAY, 0);
        register("minecraft:repeating_command_block", Blocks.REPEATING_COMMAND_BLOCK, 2, 3, 4, 5, 10, 11, 12, 13);
        register("minecraft:chain_command_block", Blocks.CHAIN_COMMAND_BLOCK, 2, 3, 4, 5, 10, 11, 12, 13);

        register("minecraft:frosted_ice", Blocks.FROSTED_ICE, 0, 1, 2, 3);
        register("minecraft:magma_block", Blocks.MAGMA, 0);
        register("minecraft:nether_wart_block", Blocks.NETHER_WART_BLOCK, 0);
        register("minecraft:red_nether_bricks", Blocks.RED_NETHER_BRICK, 0);
        register("minecraft:bone_block", Blocks.BONE_BLOCK, 0, 4, 8);
        register("minecraft:structure_void", Blocks.STRUCTURE_VOID, 0);
        register("minecraft:observer", Blocks.OBSERVER, 0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13);

        register("minecraft:white_shulker_box", Blocks.WHITE_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:orange_shulker_box", Blocks.ORANGE_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:magenta_shulker_box", Blocks.MAGENTA_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:light_blue_shulker_box", Blocks.LIGHT_BLUE_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:yellow_shulker_box", Blocks.YELLOW_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:lime_shulker_box", Blocks.LIME_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:pink_shulker_box", Blocks.PINK_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:gray_shulker_box", Blocks.GRAY_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:light_gray_shulker_box", Blocks.SILVER_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:cyan_shulker_box", Blocks.CYAN_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:purple_shulker_box", Blocks.PURPLE_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:blue_shulker_box", Blocks.BLUE_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:brown_shulker_box", Blocks.BROWN_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:green_shulker_box", Blocks.GREEN_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:red_shulker_box", Blocks.RED_SHULKER_BOX, 0, 1, 2, 3, 4, 5);
        register("minecraft:black_shulker_box", Blocks.BLACK_SHULKER_BOX, 0, 1, 2, 3, 4, 5);

        register("minecraft:white_glazed_terracotta", Blocks.WHITE_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:orange_glazed_terracotta", Blocks.ORANGE_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:magenta_glazed_terracotta", Blocks.MAGENTA_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:light_blue_glazed_terracotta", Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:yellow_glazed_terracotta", Blocks.YELLOW_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:lime_glazed_terracotta", Blocks.LIME_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:pink_glazed_terracotta", Blocks.PINK_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:gray_glazed_terracotta", Blocks.GRAY_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:light_gray_glazed_terracotta", Blocks.SILVER_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:cyan_glazed_terracotta", Blocks.CYAN_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:purple_glazed_terracotta", Blocks.PURPLE_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:blue_glazed_terracotta", Blocks.BLUE_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:brown_glazed_terracotta", Blocks.BROWN_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:green_glazed_terracotta", Blocks.GREEN_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:red_glazed_terracotta", Blocks.RED_GLAZED_TERRACOTTA, 0, 1, 2, 3);
        register("minecraft:black_glazed_terracotta", Blocks.BLACK_GLAZED_TERRACOTTA, 0, 1, 2, 3);

        register("minecraft:white_concrete", Blocks.CONCRETE, 0);
        register("minecraft:orange_concrete", Blocks.CONCRETE, 1);
        register("minecraft:magenta_concrete", Blocks.CONCRETE, 2);
        register("minecraft:light_blue_concrete", Blocks.CONCRETE, 3);
        register("minecraft:yellow_concrete", Blocks.CONCRETE, 4);
        register("minecraft:lime_concrete", Blocks.CONCRETE, 5);
        register("minecraft:pink_concrete", Blocks.CONCRETE, 6);
        register("minecraft:gray_concrete", Blocks.CONCRETE, 7);
        register("minecraft:light_gray_concrete", Blocks.CONCRETE, 8);
        register("minecraft:cyan_concrete", Blocks.CONCRETE, 9);
        register("minecraft:purple_concrete", Blocks.CONCRETE, 10);
        register("minecraft:blue_concrete", Blocks.CONCRETE, 11);
        register("minecraft:brown_concrete", Blocks.CONCRETE, 12);
        register("minecraft:green_concrete", Blocks.CONCRETE, 13);
        register("minecraft:red_concrete", Blocks.CONCRETE, 14);
        register("minecraft:black_concrete", Blocks.CONCRETE, 15);

        register("minecraft:white_concrete_powder", Blocks.CONCRETE_POWDER, 0);
        register("minecraft:orange_concrete_powder", Blocks.CONCRETE_POWDER, 1);
        register("minecraft:magenta_concrete_powder", Blocks.CONCRETE_POWDER, 2);
        register("minecraft:light_blue_concrete_powder", Blocks.CONCRETE_POWDER, 3);
        register("minecraft:yellow_concrete_powder", Blocks.CONCRETE_POWDER, 4);
        register("minecraft:lime_concrete_powder", Blocks.CONCRETE_POWDER, 5);
        register("minecraft:pink_concrete_powder", Blocks.CONCRETE_POWDER, 6);
        register("minecraft:gray_concrete_powder", Blocks.CONCRETE_POWDER, 7);
        register("minecraft:light_gray_concrete_powder", Blocks.CONCRETE_POWDER, 8);
        register("minecraft:cyan_concrete_powder", Blocks.CONCRETE_POWDER, 9);
        register("minecraft:purple_concrete_powder", Blocks.CONCRETE_POWDER, 10);
        register("minecraft:blue_concrete_powder", Blocks.CONCRETE_POWDER, 11);
        register("minecraft:brown_concrete_powder", Blocks.CONCRETE_POWDER, 12);
        register("minecraft:green_concrete_powder", Blocks.CONCRETE_POWDER, 13);
        register("minecraft:red_concrete_powder", Blocks.CONCRETE_POWDER, 14);
        register("minecraft:black_concrete_powder", Blocks.CONCRETE_POWDER, 15);

        register("minecraft:structure_block", Blocks.STRUCTURE_BLOCK, 0, 1, 2, 3);
    }
}

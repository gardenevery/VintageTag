package com.gardenevery.vintagetag;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import org.apache.commons.io.IOUtils;

/**
 * Custom Tag Loader
 * <p>
 * Can load JSON format tag files from configuration files and mods
 * <p>
 * Scans the configuration file directory: config/tags/{type}/
 * <p>
 * Scans each mod jar file within: resources/data/tags/{type}/
 * <p>
 * {type} corresponds to item/fluid/block
 * <p>
 * Unlike the official tag system in version 1.13 and above, custom tags additionally allow uppercase letters and
 * numbers in tag names and can omit namespaces (e.g. item tag "Abc_1" located in tags/item/Abc_1.json)
 * <p>
 * tags/item/a.json = item tag "a"  tags/item/a/b.json = item tag "a:b"  tags/item/a/b/c.json = item tag "a:b/c"
 *
 * Does not scan if the directory is too deeply nested (e.g., config/tags/item/a/b/c/d.json)
 * <p>
 * Supports an optional "replace" field. If "replace": true, the tag content will be replaced; otherwise,
 * it will be treated as an addition
 * <p>
 * Item tags must use the following format: { "id": "mod:name", "metadata": int }
 * <p>
 * If metadata is 0 or not present, { "id": "mod:name" } can be used
 * <p>
 * The format for fluid tags and block tags is "mod:name"
 * <p>
 * OreSync will automatically synchronize the contents of the mineral dictionary to the tag system
 */

// Directory structure:
// config/tags/                 - Root directory
//         ├── item/            - Item tags
//         ├── fluid/           - Fluid tags
//         └── block/           - Block tags
//
// resources/data/tags/         - Root directory
//                 ├── item/    - Item tags
//                 ├── fluid/   - Fluid tags
//                 └── block/   - Block tags
//
// item tag:
//{
//  "replace": true,
//  "values": [
//    { "id": "minecraft:stone", "metadata": 1 },
//    { "id": "minecraft:grass" }
//  ]
//}
//
// fluid/block tag:
//{
//  "values": [
//    "water"
//  ]
//}
@SuppressWarnings("all")
final class TagLoader {

    private static final Gson GSON = new Gson();
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+\\.json$", Pattern.CASE_INSENSITIVE);
    private static final Map<File, List<JarTagData>> CACHED_JAR_TAGS = new Object2ObjectOpenHashMap<>();
    private static boolean isInitialized = false;

    public enum Operation {
        ADD,
        REPLACE
    }

    public static void scanModTags() {
        if (!isInitialized) {
            for (var mod : Loader.instance().getModList()) {
                var source = mod.getSource();
                var modId = mod.getModId();

                if (source == null) {
                    continue;
                }
                if (source.isFile() && source.getName().endsWith(".jar")) {
                    cacheJarTags(source, modId);
                }
            }
            isInitialized = true;
        }

        for (var entry : CACHED_JAR_TAGS.entrySet()) {
            for (var tagData : entry.getValue()) {
                processTagJson(tagData.jsonObject(), tagData.tagName(), tagData.type());
            }
        }
    }

    public static void scanConfigTags() {
        scanConfigTagDirectory(new File("config", "tags/item/"), "item");
        scanConfigTagDirectory(new File("config", "tags/fluid/"), "fluid");
        scanConfigTagDirectory(new File("config", "tags/block/"), "block");
    }

    private static void cacheJarTags(File jarFile, String modId) {
        if (CACHED_JAR_TAGS.containsKey(jarFile)) {
            return;
        }

        List<JarTagData> tagList = new ArrayList<>();

        try (var zip = new ZipFile(jarFile)) {
            var entries = zip.entries();

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var entryName = entry.getName();

                if (entryName.startsWith("data/tags/") && entryName.endsWith(".json")) {
                    var parts = entryName.split("/");
                    if (parts.length >= 4) {
                        var typeString = parts[2];
                        if (!isValidTagType(typeString)) {
                            continue;
                        }

                        var fileName = parts[parts.length - 1];
                        if (!isValidJsonFileName(fileName)) {
                            continue;
                        }

                        if (parts.length - 4 <= 2 + 1) {
                            var tagName = buildTagNameFromPath(fileName, parts);
                            try (var stream = zip.getInputStream(entry)) {
                                var json = IOUtils.toString(stream, StandardCharsets.UTF_8);
                                var jsonObject = GSON.fromJson(json, JsonObject.class);

                                if (jsonObject != null) {
                                    tagList.add(new JarTagData(modId, tagName, typeString, jsonObject));
                                }
                            } catch (IOException e) {
                                TagLog.info("Failed to read tag entry from JAR: {}", entryName, e);
                            }
                        }
                    }
                }
            }

            if (!tagList.isEmpty()) {
                CACHED_JAR_TAGS.put(jarFile, tagList);
            }
        } catch (IOException e) {
            TagLog.info("Failed to scan JAR file for tags: {}", jarFile.getName(), e);
        }
    }

    private static void scanConfigTagDirectory(File directory, String tagType) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        Set<String> processedFiles = new HashSet<>();
        Deque<TagFileInfo> stack = new ArrayDeque<>();

        var rootJsonFiles = directory.listFiles((dir, name) -> isValidJsonFileName(name) && new File(dir, name).isFile());
        if (rootJsonFiles != null) {
            for (var jsonFile : rootJsonFiles) {
                var filePath = jsonFile.getAbsolutePath();
                if (processedFiles.contains(filePath)) {
                    continue;
                }
                processedFiles.add(filePath);
                var tagName = jsonFile.getName().replace(".json", "");
                processTagFile(jsonFile, tagName, tagType);
            }
        }

        var allDirs = directory.listFiles(File::isDirectory);
        if (allDirs != null) {
            for (var dir : allDirs) {
                var namespace = dir.getName();
                stack.push(new TagFileInfo(dir, namespace, "", 0));
            }
        }

        while (!stack.isEmpty()) {
            var current = stack.pop();
            var currentDir = current.file();
            var namespace = current.namespace();
            var currentPath = current.path();
            int currentDepth = current.depth();

            var jsonFiles = currentDir.listFiles((dir, name) -> isValidJsonFileName(name) && new File(dir, name).isFile());
            if (jsonFiles != null) {
                for (var jsonFile : jsonFiles) {
                    var filePath = jsonFile.getAbsolutePath();
                    if (processedFiles.contains(filePath)) {
                        continue;
                    }
                    processedFiles.add(filePath);

                    var tagNameOnly = jsonFile.getName().replace(".json", "");
                    var fullTagName = buildFullTagName(namespace, currentPath, tagNameOnly);

                    processTagFile(jsonFile, fullTagName, tagType);
                }
            }

            if (currentDepth < 2) {
                var subDirs = currentDir.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (var subDir : subDirs) {
                        var subDirName = subDir.getName();
                        var newPath = currentPath.isEmpty() ? subDirName : currentPath + "/" + subDirName;
                        stack.push(new TagFileInfo(subDir, namespace, newPath, currentDepth + 1));
                    }
                }
            }
        }
    }

    private static void processTagFile(File file, String tagName, String type) {
        try {
            var json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            var jsonObject = GSON.fromJson(json, JsonObject.class);

            if (jsonObject == null) {
                return;
            }

            processTagJson(jsonObject, tagName, type);
        } catch (Exception e) {
            TagLog.info("Failed to process {} tag file: {}", type, file.getAbsolutePath(), e);
        }
    }

    private static void processTagJson(JsonObject jsonObject, String tagName, String type) {
        if (jsonObject == null) {
            return;
        }

        if (!jsonObject.has("values") || !jsonObject.get("values").isJsonArray()) {
            TagLog.info("Invalid JSON for tag {}: missing or invalid 'values' array", tagName);
            return;
        }

        boolean replace = false;
        if (jsonObject.has("replace") && jsonObject.get("replace").isJsonPrimitive()) {
            replace = jsonObject.get("replace").getAsBoolean();
        }

        var operation = replace ? Operation.REPLACE : Operation.ADD;

        switch (type) {
            case "item":
                Set<ItemData> itemData = parseItemData(jsonObject);
                Set<ItemStack> itemStacks = createItems(itemData);
                applyItemTag(tagName, operation, itemStacks);
                break;

            case "fluid":
                Set<String> fluidNames = parseStringSet(jsonObject);
                Set<Fluid> fluids = createFluids(fluidNames);
                applyFluidTag(tagName, operation, fluids);
                break;

            case "block":
                Set<String> blockNames = parseStringSet(jsonObject);
                Set<ResourceLocation> blockLocations = parseResourceLocations(blockNames);
                Set<Block> blocks = createBlocks(blockLocations);
                applyBlockTag(tagName, operation, blocks);
                break;
        }
    }

    private static Set<ItemData> parseItemData(JsonObject jsonObject) {
        Set<ItemData> itemData = new HashSet<>();

        if (!jsonObject.has("values") || !jsonObject.get("values").isJsonArray()) {
            return itemData;
        }

        var valuesArray = jsonObject.getAsJsonArray("values");
        for (var jsonElement : valuesArray) {
            if (!jsonElement.isJsonObject()) {
                continue;
            }

            var itemObj = jsonElement.getAsJsonObject();
            if (!itemObj.has("id") || !itemObj.get("id").isJsonPrimitive()) {
                continue;
            }

            try {
                var id = itemObj.get("id").getAsString();
                int metadata = 0;

                if (itemObj.has("metadata") && itemObj.get("metadata").isJsonPrimitive()) {
                    metadata = itemObj.get("metadata").getAsInt();
                }

                itemData.add(new ItemData(new ResourceLocation(id), metadata));
            } catch (Exception e) {
                //
            }
        }
        return itemData;
    }

    private static Set<String> parseStringSet(JsonObject jsonObject) {
        Set<String> result = new HashSet<>();

        if (!jsonObject.has("values") || !jsonObject.get("values").isJsonArray()) {
            return result;
        }

        var valuesArray = jsonObject.getAsJsonArray("values");
        for (var jsonElement : valuesArray) {
            if (jsonElement.isJsonPrimitive()) {
                result.add(jsonElement.getAsString());
            }
        }
        return result;
    }

    private static Set<ResourceLocation> parseResourceLocations(Set<String> strings) {
        Set<ResourceLocation> result = new HashSet<>();

        for (var string : strings) {
            try {
                result.add(new ResourceLocation(string));
            } catch (Exception e) {
                TagLog.info("Invalid resource location: {}", string, e);
            }
        }
        return result;
    }

    private static Set<ItemStack> createItems(Set<ItemData> itemData) {
        if (itemData == null || itemData.isEmpty()) {
            return new HashSet<>();
        }

        Set<ItemStack> result = new HashSet<>();
        for (var data : itemData) {
            var item = ForgeRegistries.ITEMS.getValue(data.id());
            if (item != null) {
                int effectiveMetadata = data.metadata();
                if (!item.getHasSubtypes() && effectiveMetadata != 0) {
                    effectiveMetadata = 0;
                }
                result.add(new ItemStack(item, 1, effectiveMetadata));
            }
        }
        return result;
    }

    private static Set<Fluid> createFluids(Set<String> fluidNames) {
        if (fluidNames == null || fluidNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<Fluid> result = new HashSet<>();
        for (var fluidName : fluidNames) {
            var fluid = FluidRegistry.getFluid(fluidName);
            if (fluid != null) {
                result.add(fluid);
            }
        }
        return result;
    }

    private static Set<Block> createBlocks(Set<ResourceLocation> blockNames) {
        if (blockNames == null || blockNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<Block> result = new HashSet<>();
        for (var blockName : blockNames) {
            var block = ForgeRegistries.BLOCKS.getValue(blockName);
            if (block != null) {
                result.add(block);
            }
        }
        return result;
    }

    private static void applyItemTag(String tagName, Operation operation, Set<ItemStack> stacks) {
        switch (operation) {
            case ADD -> {
                Set<ItemKey> keys = stacks.stream()
                        .map(ItemKey::toKey)
                        .collect(Collectors.toSet());
                if (!keys.isEmpty()) {
                    TagManager.registerItem(keys, tagName);
                }
            }
            case REPLACE -> {
                TagManager.removeItem(tagName);
                Set<ItemKey> keys = stacks.stream()
                        .map(ItemKey::toKey)
                        .collect(Collectors.toSet());
                if (!keys.isEmpty()) {
                    TagManager.registerItem(keys, tagName);
                }
            }
        }
    }

    private static void applyFluidTag(String tagName, Operation operation, Set<Fluid> fluids) {
        switch (operation) {
            case ADD -> {
                if (fluids != null && !fluids.isEmpty()) {
                    TagManager.registerFluid(fluids, tagName);
                }
            }
            case REPLACE -> {
                TagManager.removeFluid(tagName);
                if (fluids != null && !fluids.isEmpty()) {
                    TagManager.registerFluid(fluids, tagName);
                }
            }
        }
    }

    private static void applyBlockTag(String tagName, Operation operation, Set<Block> blocks) {
        switch (operation) {
            case ADD -> {
                if (blocks != null && !blocks.isEmpty()) {
                    TagManager.registerBlock(blocks, tagName);
                }
            }
            case REPLACE -> {
                TagManager.removeBlock(tagName);
                if (blocks != null && !blocks.isEmpty()) {
                    TagManager.registerBlock(blocks, tagName);
                }
            }
        }
    }

    @Nonnull
    private static String buildTagNameFromPath(String fileName, String[] parts) {
        var tagName = fileName.substring(0, fileName.length() - 5);

        if (parts.length > 4) {
            var namespace = parts[3];
            var subPath = new StringBuilder();
            for (int i = 4; i < parts.length - 1; i++) {
                if (subPath.length() > 0) {
                    subPath.append("/");
                }
                subPath.append(parts[i]);
            }

            if (subPath.length() > 0) {
                tagName = namespace + ":" + subPath + "/" + tagName;
            } else {
                tagName = namespace + ":" + tagName;
            }
        }
        return tagName;
    }

    private static String buildFullTagName(String namespace, String path, String tagName) {
        if (path.isEmpty()) {
            return namespace + ":" + tagName;
        } else {
            return namespace + ":" + path + "/" + tagName;
        }
    }

    private static boolean isValidTagType(String type) {
        return "item".equals(type) || "fluid".equals(type) || "block".equals(type);
    }

    private static boolean isValidJsonFileName(String fileName) {
        return VALID_FILENAME_PATTERN.matcher(fileName).matches();
    }

    @Desugar
    private record TagFileInfo(File file, String namespace, String path, int depth) {}

    @Desugar
    private record ItemData(ResourceLocation id, int metadata) {}

    @Desugar
    private record JarTagData(String modId, String tagName, String type, JsonObject jsonObject) {}
}

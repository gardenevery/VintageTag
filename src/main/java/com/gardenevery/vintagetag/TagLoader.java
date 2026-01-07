package com.gardenevery.vintagetag;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

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
    private static final Object2ReferenceOpenHashMap<File, String> CACHED_TAG_JARS = new Object2ReferenceOpenHashMap<>();

    private static final String ITEM_TYPE = "item";
    private static final String FLUID_TYPE = "fluid";
    private static final String BLOCK_TYPE = "block";
    private static final String CONFIG = "config";
    private static final String DATA_TAGS_PREFIX = "data/tags/";
    private static final String ITEM_DIR = "tags/item/";
    private static final String FLUID_DIR = "tags/fluid/";
    private static final String BLOCK_DIR = "tags/block/";
    private static final String REPLACE = "replace";
    private static final String VALUES = "values";
    private static final String ID = "id";
    private static final String METADATA = "metadata";
    private static final String JAR = ".jar";
    private static final String JSON = ".json";
    private static final String COLON = ":";
    private static final String SLASH = "/";
    private static final String STRING_EMPTY = "";

    enum Operation {
        ADD,
        REPLACE
    }

    public static void scanModDirs() {
        for (var mod : Loader.instance().getModList()) {
            var source = mod.getSource();
            var modId = mod.getModId();

            if (source == null) {
                continue;
            }
            if (source.isFile() && source.getName().endsWith(JAR)) {
                if (jarHasTagsDir(source)) {
                    CACHED_TAG_JARS.put(source, modId);
                }
            }
        }
    }

    public static void scanModTags() {
        for (var entry : CACHED_TAG_JARS.entrySet()) {
            scanJarTags(entry.getKey(), entry.getValue());
        }
    }

    public static void scanConfigTags() {
        scanConfigTagDirectory(new File(CONFIG, ITEM_DIR), ITEM_TYPE);
        scanConfigTagDirectory(new File(CONFIG, FLUID_DIR), FLUID_TYPE);
        scanConfigTagDirectory(new File(CONFIG, BLOCK_DIR), BLOCK_TYPE);
    }

    private static boolean jarHasTagsDir(File jarFile) {
        try (var zip = new ZipFile(jarFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var entryName = entry.getName();
                if (entryName.equals(DATA_TAGS_PREFIX) || entryName.startsWith(DATA_TAGS_PREFIX) && entry.isDirectory()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            //
        }
        return false;
    }

    private static void scanConfigTagDirectory(File directory, String tagType) {
        if (!isValidDirectory(directory)) {
            return;
        }

        ObjectOpenHashSet<String> processedFiles = new ObjectOpenHashSet<>();
        Deque<FileInfo> stack = new ArrayDeque<>();

        var rootJsonFiles = directory.listFiles((dir, name) -> isValidJsonFileName(name) && new File(dir, name).isFile());
        if (rootJsonFiles != null) {
            for (var jsonFile : rootJsonFiles) {
                var filePath = jsonFile.getAbsolutePath();
                if (processedFiles.contains(filePath)) {
                    continue;
                }
                processedFiles.add(filePath);
                var tagName = jsonFile.getName().replace(JSON, STRING_EMPTY);
                processTagFile(jsonFile, tagName, tagType);
            }
        }

        var allDirs = directory.listFiles(File::isDirectory);
        if (allDirs != null) {
            for (var dir : allDirs) {
                var namespace = dir.getName();
                stack.push(new FileInfo(dir, namespace, STRING_EMPTY, 0));
            }
        }

        while (!stack.isEmpty()) {
            var current = stack.pop();
            var currentDir = current.file();
            var currentNamespace = current.namespace();
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
                    var tagName = jsonFile.getName().replace(JSON, STRING_EMPTY);
                    var fullTagName = buildTagName(currentNamespace, currentPath, tagName);
                    processTagFile(jsonFile, fullTagName, tagType);
                }
            }

            if (currentDepth < 2) {
                var subDirs = currentDir.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (var subDir : subDirs) {
                        var subDirName = subDir.getName();
                        var newPath = currentPath.isEmpty() ? subDirName : currentPath + SLASH + subDirName;
                        stack.push(new FileInfo(subDir, currentNamespace, newPath, currentDepth + 1));
                    }
                }
            }
        }
    }

    private static void scanJarTags(File jarFile, String modId) {
        try (var zip = new ZipFile(jarFile)) {
            var entries = zip.entries();

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var entryName = entry.getName();

                if (entryName.startsWith(DATA_TAGS_PREFIX) && entryName.endsWith(JSON)) {
                    var parts = entryName.split(SLASH);
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
                                processTagStream(stream, modId, tagName, typeString);
                            } catch (IOException e) {
                                TagLog.info("Failed to read tag entry from JAR: {}", entryName, e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            TagLog.info("Failed to scan JAR file for tags: {}", jarFile.getName(), e);
        }
    }

    private static boolean isValidTagType(String type) {
        return ITEM_TYPE.equals(type) || FLUID_TYPE.equals(type) || BLOCK_TYPE.equals(type);
    }

    private static void processTagFile(File file, String tagName, String type) {
        try {
            var jsonObject = parseJsonFile(file);
            if (jsonObject == null) {
                return;
            }
            processTagJson(jsonObject, tagName, type);
        } catch (Exception e) {
            TagLog.info("Failed to process {} tag file: {}", type, file.getAbsolutePath(), e);
        }
    }

    private static void processTagStream(InputStream stream, String modId, String tagName, String type) {
        try {
            var json = IOUtils.toString(stream, StandardCharsets.UTF_8);
            var jsonObject = GSON.fromJson(json, JsonObject.class);

            if (jsonObject == null) {
                return;
            }

            processTagJson(jsonObject, tagName, type);
        } catch (IOException e) {
            TagLog.info("Failed to read stream for tag {} from mod {}: {}", tagName, modId, e.getMessage());
        } catch (Exception e) {
            TagLog.info("Failed to process tag stream for {}:{}", modId, tagName, e);
        }
    }

    private static void processTagJson(JsonObject jsonObject, String tagName, String type) {
        if (jsonObject == null) {
            return;
        }

        if (!jsonObject.has(VALUES) || !jsonObject.get(VALUES).isJsonArray()) {
            TagLog.info("Invalid JSON for tag {}: missing or invalid 'values' array", tagName);
            return;
        }

        boolean replace = false;
        if (jsonObject.has(REPLACE) && jsonObject.get(REPLACE).isJsonPrimitive()) {
            replace = jsonObject.get(REPLACE).getAsBoolean();
        }

        var operation = replace ? Operation.REPLACE : Operation.ADD;

        switch (type) {
            case ITEM_TYPE:
                ObjectOpenHashSet<ItemData> itemData = parseItemData(jsonObject);
                ObjectOpenHashSet<ItemStack> itemStacks = createItems(itemData);
                applyItemTag(tagName, operation, itemStacks);
                break;

            case FLUID_TYPE:
                ObjectOpenHashSet<String> fluidNames = parseStringSet(jsonObject);
                ObjectOpenHashSet<Fluid> fluids = createFluids(fluidNames);
                applyFluidTag(tagName, operation, fluids);
                break;

            case BLOCK_TYPE:
                ObjectOpenHashSet<String> blockNames = parseStringSet(jsonObject);
                ObjectOpenHashSet<ResourceLocation> blockLocations = parseResourceLocations(blockNames);
                ObjectOpenHashSet<Block> blocks = createBlocks(blockLocations);
                applyBlockTag(tagName, operation, blocks);
                break;
        }
    }

    private static JsonObject parseJsonFile(File file) {
        try {
            var json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return GSON.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            TagLog.info("Failed to parse JSON file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    private static ObjectOpenHashSet<ItemData> parseItemData(JsonObject jsonObject) {
        ObjectOpenHashSet<ItemData> itemData = new ObjectOpenHashSet<>();

        if (!jsonObject.has(VALUES) || !jsonObject.get(VALUES).isJsonArray()) {
            return itemData;
        }

        var valuesArray = jsonObject.getAsJsonArray(VALUES);
        for (var jsonElement : valuesArray) {
            if (!jsonElement.isJsonObject()) {
                continue;
            }

            var itemObj = jsonElement.getAsJsonObject();
            if (!itemObj.has(ID) || !itemObj.get(ID).isJsonPrimitive()) {
                continue;
            }

            try {
                var id = itemObj.get(ID).getAsString();
                int metadata = 0;

                if (itemObj.has(METADATA) && itemObj.get(METADATA).isJsonPrimitive()) {
                    metadata = itemObj.get(METADATA).getAsInt();
                }

                itemData.add(new ItemData(new ResourceLocation(id), metadata));
            } catch (Exception e) {
                //
            }
        }
        return itemData;
    }

    private static ObjectOpenHashSet<String> parseStringSet(JsonObject jsonObject) {
        ObjectOpenHashSet<String> result = new ObjectOpenHashSet<>();

        if (!jsonObject.has(VALUES) || !jsonObject.get(VALUES).isJsonArray()) {
            return result;
        }

        var valuesArray = jsonObject.getAsJsonArray(VALUES);
        for (var jsonElement : valuesArray) {
            if (jsonElement.isJsonPrimitive()) {
                result.add(jsonElement.getAsString());
            }
        }
        return result;
    }

    private static ObjectOpenHashSet<ResourceLocation> parseResourceLocations(ObjectOpenHashSet<String> strings) {
        ObjectOpenHashSet<ResourceLocation> result = new ObjectOpenHashSet<>();

        for (var string : strings) {
            try {
                result.add(new ResourceLocation(string));
            } catch (Exception e) {
                TagLog.info("Invalid resource location: {}", string, e);
            }
        }
        return result;
    }

    private static ObjectOpenHashSet<ItemStack> createItems(ObjectOpenHashSet<ItemData> itemData) {
        if (itemData == null || itemData.isEmpty()) {
            return new ObjectOpenHashSet<>();
        }

        ObjectOpenHashSet<ItemStack> result = new ObjectOpenHashSet<>();
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

    private static ObjectOpenHashSet<Fluid> createFluids(ObjectOpenHashSet<String> fluidNames) {
        if (fluidNames == null || fluidNames.isEmpty()) {
            return new ObjectOpenHashSet<>();
        }

        ObjectOpenHashSet<Fluid> result = new ObjectOpenHashSet<>();
        for (var fluidName : fluidNames) {
            var fluid = FluidRegistry.getFluid(fluidName);
            if (fluid != null) {
                result.add(fluid);
            }
        }
        return result;
    }

    private static ObjectOpenHashSet<Block> createBlocks(ObjectOpenHashSet<ResourceLocation> blockNames) {
        if (blockNames == null || blockNames.isEmpty()) {
            return new ObjectOpenHashSet<>();
        }

        ObjectOpenHashSet<Block> result = new ObjectOpenHashSet<>();
        for (var blockName : blockNames) {
            var block = ForgeRegistries.BLOCKS.getValue(blockName);
            if (block != null) {
                result.add(block);
            }
        }
        return result;
    }

    private static void applyItemTag(String tagName, Operation operation, ObjectOpenHashSet<ItemStack> stacks) {
        switch (operation) {
            case ADD -> {
                Set<ItemKey> keys = ItemKey.toKey(stacks);
                if (!keys.isEmpty()) {
                    TagManager.item().register(keys, tagName);
                }
            }
            case REPLACE -> {
                TagManager.item().remove(tagName);
                Set<ItemKey> keys = ItemKey.toKey(stacks);
                if (!keys.isEmpty()) {
                    TagManager.item().register(keys, tagName);
                }
            }
        }
    }

    private static void applyFluidTag(String tagName, Operation operation, ObjectOpenHashSet<Fluid> fluids) {
        switch (operation) {
            case ADD -> {
                if (fluids != null && !fluids.isEmpty()) {
                    TagManager.fluid().register(fluids, tagName);
                }
            }
            case REPLACE -> {
                TagManager.fluid().remove(tagName);
                if (fluids != null && !fluids.isEmpty()) {
                    TagManager.fluid().register(fluids, tagName);
                }
            }
        }
    }

    private static void applyBlockTag(String tagName, Operation operation, ObjectOpenHashSet<Block> blocks) {
        switch (operation) {
            case ADD -> {
                if (blocks != null && !blocks.isEmpty()) {
                    TagManager.block().register(blocks, tagName);
                }
            }
            case REPLACE -> {
                TagManager.block().remove(tagName);
                if (blocks != null && !blocks.isEmpty()) {
                    TagManager.block().register(blocks, tagName);
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
                    subPath.append(SLASH);
                }
                subPath.append(parts[i]);
            }

            if (subPath.length() > 0) {
                tagName = namespace + COLON + subPath + SLASH + tagName;
            } else {
                tagName = namespace + COLON + tagName;
            }
        }
        return tagName;
    }

    private static String buildTagName(String namespace, String path, String tagName) {
        if (path.isEmpty()) {
            return namespace + COLON + tagName;
        } else {
            return namespace + COLON + path + SLASH + tagName;
        }
    }

    private static boolean isValidDirectory(File directory) {
        return directory.exists() && directory.isDirectory();
    }

    private static boolean isValidJsonFileName(String fileName) {
        return VALID_FILENAME_PATTERN.matcher(fileName).matches();
    }

    @Desugar
    private record FileInfo(File file, String namespace, String path, int depth) {}

    @Desugar
    private record ItemData(ResourceLocation id, int metadata) {}
}

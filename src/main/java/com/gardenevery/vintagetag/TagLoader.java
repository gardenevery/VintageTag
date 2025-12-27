package com.gardenevery.vintagetag;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public final class TagLoader {

    enum Operation {
        ADD,
        REPLACE
    }

    private static final Logger LOGGER = LogManager.getLogger("TagLoader");
    private static final Gson GSON = new Gson();
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+\\.json$", Pattern.CASE_INSENSITIVE);

    private static final String CONFIG = "config";
    private static final String ITEM_DIR = "tags/item/";
    private static final String FLUID_DIR = "tags/fluid/";
    private static final String BLOCK_DIR = "tags/block/";
    private static final String REPLACE = "replace";
    private static final String VALUES = "values";
    private static final String ID = "id";
    private static final String METADATA = "metadata";
    private static final String JSON = ".json";
    private static final String COLON = ":";
    private static final String SLASH = "/";
    private static final String STRING_EMPTY = "";

    public static void scanModTags() {
        for (var mod : Loader.instance().getModList()) {
            var source = mod.getSource();
            var modId = mod.getModId();

            if (source == null) {
                continue;
            }

            if (source.isFile() && source.getName().endsWith(".jar")) {
                scanJarTags(source, modId);
            }
        }
    }

    public static void scanConfigTags() {
        scanConfigItemTag(new File(CONFIG, ITEM_DIR));
        scanConfigFluidTag(new File(CONFIG, FLUID_DIR));
        scanConfigBlockTag(new File(CONFIG, BLOCK_DIR));
    }

    private static void scanConfigItemTag(File directory) {
        if (!isValidDirectory(directory)) {
            return;
        }
        scanConfigTagDirectory(directory, TagType.ITEM);
    }

    private static void scanConfigFluidTag(File directory) {
        if (!isValidDirectory(directory)) {
            return;
        }
        scanConfigTagDirectory(directory, TagType.FLUID);
    }

    private static void scanConfigBlockTag(File directory) {
        if (!isValidDirectory(directory)) {
            return;
        }
        scanConfigTagDirectory(directory, TagType.BLOCK);
    }

    private static void scanConfigTagDirectory(File directory, TagType tagType) {
        if (!isValidDirectory(directory)) {
            return;
        }

        Set<String> processedFiles = new HashSet<>();
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
                    String fullTagName = buildTagName(currentNamespace, currentPath, tagName);
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
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var entryName = entry.getName();

                if (entryName.startsWith("data/tags/") && entryName.endsWith(".json")) {
                    var parts = entryName.split("/");
                    if (parts.length >= 4) {
                        var typeStr = parts[2];
                        var type = TagType.getType(typeStr);

                        if (type != null) {
                            var fileName = parts[parts.length - 1];

                            if (!isValidJsonFileName(fileName)) {
                                continue;
                            }

                            if (parts.length - 4 <= 2 + 1) {
                                var tagName = buildTagNameFromPath(fileName, parts);
                                try (var stream = zip.getInputStream(entry)) {
                                    processTagStream(stream, modId, tagName, type);
                                } catch (IOException e) {
                                    LOGGER.info("Failed to read tag entry from JAR: {}", entryName, e);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.info("Failed to scan JAR file for tags: {}", jarFile.getName(), e);
        }
    }

    private static void processTagFile(File file, String tagName, TagType type) {
        try {
            var jsonObject = parseJsonFile(file);
            if (jsonObject == null) {
                return;
            }
            processTagJson(jsonObject, tagName, type);
        } catch (Exception e) {
            LOGGER.info("Failed to process {} tag file: {}", type, file.getAbsolutePath(), e);
        }
    }

    private static void processTagStream(InputStream stream, String modId, String tagName, TagType type) {
        try {
            var json = IOUtils.toString(stream, StandardCharsets.UTF_8);
            var jsonObject = GSON.fromJson(json, JsonObject.class);

            if (jsonObject == null) {
                return;
            }

            processTagJson(jsonObject, tagName, type);
        } catch (IOException e) {
            LOGGER.info("Failed to read stream for tag {} from mod {}: {}", tagName, modId, e.getMessage());
        } catch (Exception e) {
            LOGGER.info("Failed to process tag stream for {}:{}", modId, tagName, e);
        }
    }

    private static void processTagJson(JsonObject jsonObject, String tagName, TagType type) {
        if (jsonObject == null) {
            return;
        }

        if (!jsonObject.has(VALUES) || !jsonObject.get(VALUES).isJsonArray()) {
            LOGGER.info("Invalid JSON for tag {}: missing or invalid 'values' array", tagName);
            return;
        }

        boolean replace = false;
        if (jsonObject.has(REPLACE) && jsonObject.get(REPLACE).isJsonPrimitive()) {
            replace = jsonObject.get(REPLACE).getAsBoolean();
        }

        var operation = replace ? Operation.REPLACE : Operation.ADD;

        switch (type) {
            case ITEM:
                Set<ItemData> itemData = parseItemData(jsonObject);
                Set<ItemStack> itemStacks = createItems(itemData);
                applyItemTag(tagName, operation, itemStacks);
                break;

            case FLUID:
                Set<String> fluidNames = parseStringSet(jsonObject);
                Set<Fluid> fluids = createFluids(fluidNames);
                applyFluidTag(tagName, operation, fluids);
                break;

            case BLOCK:
                Set<String> blockNames = parseStringSet(jsonObject);
                Set<ResourceLocation> blockLocations = parseResourceLocations(blockNames);
                Set<Block> blocks = createBlocks(blockLocations);
                applyBlockTag(tagName, operation, blocks);
                break;
        }
    }

    private static JsonObject parseJsonFile(File file) {
        try {
            var json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return GSON.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            LOGGER.info("Failed to parse JSON file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    private static Set<ItemData> parseItemData(JsonObject jsonObject) {
        Set<ItemData> itemData = new HashSet<>();

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

    private static Set<String> parseStringSet(JsonObject jsonObject) {
        Set<String> result = new HashSet<>();

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

    private static Set<ResourceLocation> parseResourceLocations(Set<String> strings) {
        Set<ResourceLocation> result = new HashSet<>();

        for (var str : strings) {
            try {
                result.add(new ResourceLocation(str));
            } catch (Exception e) {
                LOGGER.info("Invalid resource location: {}", str, e);
            }
        }
        return result;
    }

    private static Set<ItemStack> createItems(Set<ItemData> itemData) {
        if (itemData == null || itemData.isEmpty()) {
            return Collections.emptySet();
        }

        Set<ItemStack> result = new HashSet<>();
        for (var data : itemData) {
            var item = ForgeRegistries.ITEMS.getValue(data.id());
            if (item != null) {
                result.add(new ItemStack(item, 1, data.metadata()));
            }
        }
        return result;
    }

    private static Set<Fluid> createFluids(Set<String> fluidNames) {
        if (fluidNames == null || fluidNames.isEmpty()) {
            return Collections.emptySet();
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
            return Collections.emptySet();
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
                Set<ItemKey> keys = ItemKey.toKeys(stacks);
                if (!keys.isEmpty()) {
                    TagManager.ITEM.createTag(tagName, keys);
                }
            }
            case REPLACE -> {
                TagManager.ITEM.remove(tagName);
                Set<ItemKey> keys = ItemKey.toKeys(stacks);
                if (!keys.isEmpty()) {
                    TagManager.ITEM.createTag(tagName, keys);
                }
            }
        }
    }

    private static void applyFluidTag(String tagName, Operation operation, Set<Fluid> fluids) {
        switch (operation) {
            case ADD -> {
                if (fluids != null && !fluids.isEmpty()) {
                    TagManager.FLUID.createTag(tagName, fluids);
                }
            }
            case REPLACE -> {
                TagManager.FLUID.remove(tagName);
                if (fluids != null && !fluids.isEmpty()) {
                    TagManager.FLUID.createTag(tagName, fluids);
                }
            }
        }
    }

    private static void applyBlockTag(String tagName, Operation operation, Set<Block> blocks) {
        switch (operation) {
            case ADD -> {
                if (blocks != null && !blocks.isEmpty()) {
                    TagManager.BLOCK.createTag(tagName, blocks);
                }
            }
            case REPLACE -> {
                TagManager.BLOCK.remove(tagName);
                if (blocks != null && !blocks.isEmpty()) {
                    TagManager.BLOCK.createTag(tagName, blocks);
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
                tagName = namespace + COLON + subPath + "/" + tagName;
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

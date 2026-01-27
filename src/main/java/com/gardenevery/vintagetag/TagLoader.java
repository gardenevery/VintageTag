package com.gardenevery.vintagetag;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.block.Block;
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
 * <p>
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
final class TagLoader {
    private static final Gson GSON = new Gson();
    private static final int MAX_DIRECTORY_DEPTH = 3;
    private static volatile boolean isInitialized = false;
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+\\.json$", Pattern.CASE_INSENSITIVE);
    private static final Map<File, List<JarTagData>> CACHED_JAR_TAGS = new Object2ObjectOpenHashMap<>();

    public enum Operation {
        ADD,
        REPLACE
    }

    public static void scanModTags() {
        if (isInitialized) {
            return;
        }

        for (var mod : Loader.instance().getModList()) {
            var source = mod.getSource();
            if (source == null || !source.isFile() || !source.getName().endsWith(".jar")) {
                continue;
            }
            cacheJarTags(source, mod.getModId());
        }

        CACHED_JAR_TAGS.forEach((jarFile, tagList) -> {
            for (var tagData : tagList) {
                processTagJson(tagData.jsonObject(), tagData.tagName(), tagData.type());
            }
        });
        isInitialized = true;
    }

    public static void scanConfigTags() {
        scanTagDirectory(Paths.get("config", "tags"), TagLoader::processConfigTagFile);
    }

    private static void cacheJarTags(File jarFile, String modId) {
        if (CACHED_JAR_TAGS.containsKey(jarFile)) {
            return;
        }

        List<JarTagData> tagList = new ArrayList<>();

        try (var zip = new ZipFile(jarFile)) {
            zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith("data/tags/") && entry.getName().endsWith(".json"))
                    .forEach(entry -> processJarEntry(entry, modId, zip, tagList));
        } catch (IOException e) {
            TagLog.info("Failed to scan JAR file for tags: {}", jarFile.getName(), e);
        }

        if (!tagList.isEmpty()) {
            CACHED_JAR_TAGS.put(jarFile, tagList);
        }
    }

    private static void processJarEntry(ZipEntry entry, String modId, ZipFile zip, List<JarTagData> tagList) {
        var entryName = entry.getName();
        var parts = entryName.split("/");

        if (parts.length < 4) {
            return;
        }

        var tagType = TagType.getType(parts[2]);
        if (tagType == null) {
            return;
        }

        var fileName = parts[parts.length - 1];
        if (!isValidJsonFileName(fileName)) {
            return;
        }

        if (parts.length - 4 > MAX_DIRECTORY_DEPTH) {
            return;
        }

        var tagName = buildTagNameFromPath(fileName, parts);
        try (var stream = zip.getInputStream(entry)) {
            var json = IOUtils.toString(stream, StandardCharsets.UTF_8);
            var jsonObject = GSON.fromJson(json, JsonObject.class);

            if (jsonObject != null) {
                tagList.add(new JarTagData(modId, tagName, tagType, jsonObject));
            }
        } catch (IOException e) {
            TagLog.info("Failed to read tag entry from JAR: {}", entryName, e);
        }
    }

    private static void scanTagDirectory(Path rootDir, TagFileProcessor processor) {
        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            return;
        }

        for (var type : TagType.values()) {
            var typeDir = rootDir.resolve(type.getName());
            if (Files.exists(typeDir) && Files.isDirectory(typeDir)) {
                scanTypeDirectory(typeDir, type, processor);
            }
        }
    }

    private static void scanTypeDirectory(Path typeDir, TagType type, TagFileProcessor processor) {
        try (var paths = Files.walk(typeDir, MAX_DIRECTORY_DEPTH)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                    var relativePath = typeDir.relativize(path).toString().replace(File.separatorChar, '/');
                    var tagName = convertPathToTagName(relativePath);
                    processor.process(path, tagName, type);
                });
        } catch (IOException e) {
            TagLog.info("Failed to scan directory: {}", typeDir, e);
        }
    }

    private static void processConfigTagFile(Path file, String tagName, TagType type) {
        try {
             byte[] bytes = Files.readAllBytes(file);
             var json = new String(bytes, StandardCharsets.UTF_8);

            var jsonObject = GSON.fromJson(json, JsonObject.class);

            if (jsonObject != null) {
                processTagJson(jsonObject, tagName, type);
            }
        } catch (Exception e) {
            TagLog.info("Failed to process {} tag file: {}", type.name().toLowerCase(), file, e);
        }
    }

    private static void processTagJson(JsonObject jsonObject, String tagName, TagType type) {
        if (jsonObject == null || !jsonObject.has("values") || !jsonObject.get("values").isJsonArray()) {
            TagLog.info("Invalid JSON for tag {}: missing or invalid 'values' array", tagName);
            return;
        }

        boolean replace = jsonObject.has("replace") && jsonObject.get("replace").isJsonPrimitive() && jsonObject.get("replace").getAsBoolean();
        var operation = replace ? Operation.REPLACE : Operation.ADD;

        switch (type) {
            case ITEM -> processItemTag(tagName, operation, jsonObject);
            case FLUID -> processFluidTag(tagName, operation, jsonObject);
            case BLOCK -> processBlockTag(tagName, operation, jsonObject);
        }
    }

    private static void processItemTag(String tagName, Operation operation, JsonObject jsonObject) {
        var itemEntries = parseItemEntries(jsonObject);
        if (itemEntries.isEmpty()) {
            return;
        }

        var itemKeys = createItemKeys(itemEntries);
        if (itemKeys.isEmpty()) {
            return;
        }
        applyTagOperation(tagName, operation, itemKeys, TagManager::registerItem, TagManager::replaceItem);
    }

    private static void processFluidTag(String tagName, Operation operation, JsonObject jsonObject) {
        var fluidNames = parseStringEntries(jsonObject);
        if (fluidNames.isEmpty()) {
            return;
        }

        var fluids = createFluids(fluidNames);
        if (fluids.isEmpty()) {
            return;
        }
        applyTagOperation(tagName, operation, fluids, TagManager::registerFluid, TagManager::replaceFluid);
    }

    private static void processBlockTag(String tagName, Operation operation, JsonObject jsonObject) {
        var blockNames = parseStringEntries(jsonObject);
        if (blockNames.isEmpty()) {
            return;
        }

        var blocks = createBlocks(blockNames);
        if (blocks.isEmpty()) {
            return;
        }
        applyTagOperation(tagName, operation, blocks, TagManager::registerBlock, TagManager::replaceBlock);
    }

    private static Set<ItemEntry> parseItemEntries(JsonObject jsonObject) {
        var valuesArray = jsonObject.getAsJsonArray("values");
        if (valuesArray == null) {
            return Collections.emptySet();
        }

        Set<ItemEntry> entries = new HashSet<>();
        for (var element : valuesArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            var itemObj = element.getAsJsonObject();
            if (!itemObj.has("id") || !itemObj.get("id").isJsonPrimitive()) {
                continue;
            }

            try {
                var id = itemObj.get("id").getAsString();
                int metadata = itemObj.has("metadata") && itemObj.get("metadata").isJsonPrimitive() ? itemObj.get("metadata").getAsInt() : 0;
                entries.add(new ItemEntry(id, metadata));
            } catch (Exception e) {
                //
            }
        }
        return entries;
    }

    private static Set<String> parseStringEntries(JsonObject jsonObject) {
        var valuesArray = jsonObject.getAsJsonArray("values");
        if (valuesArray == null) {
            return Collections.emptySet();
        }

        Set<String> entries = new HashSet<>();
        for (var element : valuesArray) {
            if (element.isJsonPrimitive()) {
                entries.add(element.getAsString());
            }
        }
        return entries;
    }

    private static Set<ItemKey> createItemKeys(Set<ItemEntry> itemEntries) {
        return itemEntries.stream().map(entry -> {
                var id = parseResourceLocation(entry.id());
                if (id == null) {
                    return null;
                }

                var item = ForgeRegistries.ITEMS.getValue(id);
                if (item == null) {
                    return null;
                }

                int metadata = entry.metadata();
                if (!item.getHasSubtypes() && metadata != 0) {
                    metadata = 0;
                }
                return ItemKey.of(item, metadata);
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    }

    private static Set<Fluid> createFluids(Set<String> fluidNames) {
        return fluidNames.stream()
                .map(FluidRegistry::getFluid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Set<Block> createBlocks(Set<String> blockNames) {
        return blockNames.stream()
                .map(TagLoader::parseResourceLocation)
                .filter(Objects::nonNull)
                .map(ForgeRegistries.BLOCKS::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static <T> void applyTagOperation(String tagName, Operation operation, Set<T> items, BiConsumer<Set<T>, String> register,
                                              BiConsumer<Set<T>, String> replace) {
        switch (operation) {
            case ADD -> register.accept(items, tagName);
            case REPLACE -> replace.accept(items, tagName);
        }
    }

    @Nullable
    private static ResourceLocation parseResourceLocation(String string) {
        try {
            return new ResourceLocation(string);
        } catch (Exception e) {
            TagLog.info("Invalid resource location: {}", string, e);
            return null;
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
                return namespace + ":" + subPath + "/" + tagName;
            } else {
                return namespace + ":" + tagName;
            }
        }
        return tagName;
    }

    private static String convertPathToTagName(String relativePath) {
        var parts = relativePath.substring(0, relativePath.length() - 5).split("/");
        return parts.length == 1 ? parts[0] : parts[0] + ":" + String.join("/", Arrays.copyOfRange(parts, 1, parts.length));
    }

    private static boolean isValidJsonFileName(String fileName) {
        return VALID_FILENAME_PATTERN.matcher(fileName).matches();
    }

    @FunctionalInterface
    private interface TagFileProcessor {
        void process(Path file, String tagName, TagType type);
    }

    @FunctionalInterface
    private interface BiConsumer<T, U> {
        void accept(T t, U u);
    }

    @Desugar
    private record ItemEntry(String id, int metadata) {}

    @Desugar
    private record JarTagData(String modId, String tagName, TagType type, JsonObject jsonObject) {}
}

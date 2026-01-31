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
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
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
 * <strong>Tag References:</strong> The tag system supports referencing other tags within the "values" array.
 * Prefix a tag name with "#" to include all entries from that tag. For example, "#minecraft:stones" will
 * include all items/blocks/fluids from the "minecraft:stones" tag. This allows for hierarchical tag organization
 * and reduces duplication. Note: Circular references should be avoided.
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
// {
//   "replace": true,
//   "values": [
//     { "id": "minecraft:stone", "metadata": 1 },
//     "minecraft:grass",
//     "#minecraft:stones"  // Reference to another tag
//   ]
// }
//
// fluid/block tag:
// {
//   "values": [
//     "water",
//     "#forge:lava"  // Reference to another fluid tag
//   ]
// }
final class TagLoader {

    private static final Gson GSON = new Gson();
    private static volatile boolean isInitialized = false;
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]+\\.json$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Map<File, List<JarTagData>> JAR_TAG_CACHED = new Object2ObjectOpenHashMap<>();

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

        JAR_TAG_CACHED.forEach((jarFile, tagList) -> {
            for (var tagData : tagList) {
                processTagJson(tagData.jsonObject(), tagData.tagName(), tagData.type());
            }
        });

        isInitialized = true;
    }

    public static void scanConfigTags() {
        scanConfigTagDirectory(Paths.get("config", "tags"));
    }

    private static void cacheJarTags(File jarFile, String modId) {
        if (JAR_TAG_CACHED.containsKey(jarFile)) {
            return;
        }

        List<JarTagData> tagList = new ArrayList<>();

        try (var zip = new ZipFile(jarFile)) {
            var entries = zip.entries();

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    var name = entry.getName();

                    if (name.startsWith("data/tags/") && name.endsWith(".json")) {
                        processJarEntry(entry, modId, zip, tagList);
                    }
                }
            }
        } catch (IOException e) {
            TagLog.info("Failed to scan JAR file for tags: {}", jarFile.getName(), e);
        }

        if (!tagList.isEmpty()) {
            JAR_TAG_CACHED.put(jarFile, tagList);
        }
    }

    private static void processJarEntry(
            ZipEntry entry,
            String modId,
            ZipFile zip,
            List<JarTagData> tagList
    ) {
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

        if (parts.length - 4 > 3) {
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

    private static void scanConfigTagDirectory(Path rootDir) {
        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            return;
        }

        for (var type : TagType.values()) {
            var typeDir = rootDir.resolve(type.getName());
            if (Files.exists(typeDir) && Files.isDirectory(typeDir)) {
                scanConfigTypeDirectory(typeDir, type);
            }
        }
    }

    private static void scanConfigTypeDirectory(Path typeDir, TagType type) {
        try (var paths = Files.walk(typeDir, 3)) {
            var iterator = paths.iterator();

            while (iterator.hasNext()) {
                var path = iterator.next();

                if (Files.isRegularFile(path)) {
                    var fileName = path.getFileName().toString();

                    if (fileName.endsWith(".json")) {
                        var relativePath = typeDir.relativize(path).toString()
                                .replace(File.separatorChar, '/');
                        var tagName = convertPathToTagName(relativePath);
                        processConfigTagFile(path, tagName, type);
                    }
                }
            }
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
            TagLog.info(
                    "Failed to process {} tag file: {}",
                    type.name().toLowerCase(),
                    file,
                    e
            );
        }
    }

    private static void processTagJson(JsonObject jsonObject, String tagName, TagType type) {
        if (
                jsonObject == null ||
                        !jsonObject.has("values") ||
                        !jsonObject.get("values").isJsonArray()
        ) {
            TagLog.info("Invalid JSON for tag {}: missing or invalid 'values' array", tagName);
            return;
        }

        boolean replace = jsonObject.has("replace") &&
                jsonObject.get("replace").isJsonPrimitive() &&
                jsonObject.get("replace").getAsBoolean();

        var operation = replace ? Operation.REPLACE : Operation.ADD;

        switch (type) {
            case ITEM -> processItemTag(tagName, operation, jsonObject);
            case FLUID -> processFluidTag(tagName, operation, jsonObject);
            case BLOCK -> processBlockTag(tagName, operation, jsonObject);
        }
    }

    private static void processItemTag(String tagName, Operation operation, JsonObject jsonObject) {
        var parseResult = parseItemEntries(jsonObject);
        var itemKeys = createItemKeys(parseResult.directEntries());

        var tagReferences = parseResult.tagReferences();

        if (operation == Operation.ADD) {
            TagManager.registerItem(itemKeys, tagName, tagReferences);
        } else {
            TagManager.replaceItem(itemKeys, tagName, tagReferences);
        }
    }

    private static void processFluidTag(String tagName, Operation operation, JsonObject jsonObject) {
        var parseResult = parseFluidEntries(jsonObject);
        var fluids = createFluids(parseResult.directEntries());

        var tagReferences = parseResult.tagReferences();

        if (operation == Operation.ADD) {
            TagManager.registerFluid(fluids, tagName, tagReferences);
        } else {
            TagManager.replaceFluid(fluids, tagName, tagReferences);
        }
    }

    private static void processBlockTag(String tagName, Operation operation, JsonObject jsonObject) {
        var parseResult = parseBlockEntries(jsonObject);
        var blocks = createBlocks(parseResult.directEntries());

        var tagReferences = parseResult.tagReferences();

        if (operation == Operation.ADD) {
            TagManager.registerBlock(blocks, tagName, tagReferences);
        } else {
            TagManager.replaceBlock(blocks, tagName, tagReferences);
        }
    }

    private static ItemParseResult parseItemEntries(JsonObject jsonObject) {
        var valuesArray = jsonObject.getAsJsonArray("values");
        if (valuesArray == null) {
            return new ItemParseResult(Collections.emptySet(), Collections.emptySet());
        }

        Set<ItemEntry> directEntries = new HashSet<>();
        Set<String> tagReferences = new HashSet<>();

        for (var element : valuesArray) {
            try {
                if (element.isJsonObject()) {
                    var itemObj = element.getAsJsonObject();
                    if (!itemObj.has("id") || !itemObj.get("id").isJsonPrimitive()) {
                        continue;
                    }

                    var id = itemObj.get("id").getAsString();

                    if (id.startsWith("#")) {
                        tagReferences.add(id.substring(1));
                        continue;
                    }

                    int metadata = itemObj.has("metadata") && itemObj.get("metadata").isJsonPrimitive() ?
                            itemObj.get("metadata").getAsInt() : 0;

                    directEntries.add(new ItemEntry(id, metadata));
                } else if (element.isJsonPrimitive()) {
                    var value = element.getAsString();

                    if (value.startsWith("#")) {
                        tagReferences.add(value.substring(1));
                    } else {
                        directEntries.add(new ItemEntry(value, 0));
                    }
                }
            } catch (Exception e) {
                //
            }
        }

        return new ItemParseResult(directEntries, tagReferences);
    }

    private static FluidParseResult parseFluidEntries(JsonObject jsonObject) {
        var valuesArray = jsonObject.getAsJsonArray("values");
        if (valuesArray == null) {
            return new FluidParseResult(Collections.emptySet(), Collections.emptySet());
        }

        Set<String> directEntries = new HashSet<>();
        Set<String> tagReferences = new HashSet<>();

        for (var element : valuesArray) {
            if (element.isJsonPrimitive()) {
                var value = element.getAsString();

                if (value.startsWith("#")) {
                    tagReferences.add(value.substring(1));
                } else {
                    directEntries.add(value);
                }
            }
        }

        return new FluidParseResult(directEntries, tagReferences);
    }

    private static BlockParseResult parseBlockEntries(JsonObject jsonObject) {
        var valuesArray = jsonObject.getAsJsonArray("values");
        if (valuesArray == null) {
            return new BlockParseResult(Collections.emptySet(), Collections.emptySet());
        }

        Set<String> directEntries = new HashSet<>();
        Set<String> tagReferences = new HashSet<>();

        for (var element : valuesArray) {
            if (element.isJsonPrimitive()) {
                var value = element.getAsString();

                if (value.startsWith("#")) {
                    tagReferences.add(value.substring(1));
                } else {
                    directEntries.add(value);
                }
            }
        }

        return new BlockParseResult(directEntries, tagReferences);
    }

    private static Set<ItemKey> createItemKeys(Set<ItemEntry> itemEntries) {
        Set<ItemKey> result = new HashSet<>(itemEntries.size());

        for (var entry : itemEntries) {
            var resourceLocation = new ResourceLocation(entry.id());
            var item = ForgeRegistries.ITEMS.getValue(resourceLocation);

            if (item == null) {
                continue;
            }

            if (entry.metadata() < 0) {
                continue;
            }

            var stack = new ItemStack(item, 1, entry.metadata());
            if (stack.isEmpty()) {
                continue;
            }

            result.add(ItemKey.of(stack));
        }

        return result;
    }

    private static Set<Fluid> createFluids(Set<String> fluidNames) {
        Set<Fluid> result = new HashSet<>(fluidNames.size());

        for (var name : fluidNames) {
            var fluid = FluidRegistry.getFluid(name);
            if (fluid != null) {
                result.add(fluid);
            }
        }

        return result;
    }

    private static Set<Block> createBlocks(Set<String> blockNames) {
        Set<Block> result = new HashSet<>(blockNames.size());

        for (var name : blockNames) {
            var resourceLocation = new ResourceLocation(name);
            var block = ForgeRegistries.BLOCKS.getValue(resourceLocation);

            if (block != null) {
                result.add(block);
            }
        }

        return result;
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
        return parts.length == 1 ?
                parts[0] :
                parts[0] + ":" + String.join("/", Arrays.copyOfRange(parts, 1, parts.length));
    }

    private static boolean isValidJsonFileName(String fileName) {
        return VALID_FILENAME_PATTERN.matcher(fileName).matches();
    }

    @Desugar
    private record ItemEntry(String id, int metadata) {}

    @Desugar
    private record JarTagData(String modId, String tagName, TagType type, JsonObject jsonObject) {}

    @Desugar
    private record ItemParseResult(Set<ItemEntry> directEntries, Set<String> tagReferences) {}

    @Desugar
    private record FluidParseResult(Set<String> directEntries, Set<String> tagReferences) {}

    @Desugar
    private record BlockParseResult(Set<String> directEntries, Set<String> tagReferences) {}
}

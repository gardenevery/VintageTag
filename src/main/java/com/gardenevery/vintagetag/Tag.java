package com.gardenevery.vintagetag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

final class Tag<T> {

    private final Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys = new Object2ReferenceOpenHashMap<>();
    private final Object2ReferenceOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags = new Object2ReferenceOpenHashMap<>();

    @Nonnull
    public Set<String> getTags(@Nonnull T key) {
        var tags = keyToTags.get(key);
        return tags == null || tags.isEmpty() ? Collections.emptySet() : new HashSet<>(tags);
    }

    @Nonnull
    public Set<T> getKeys(@Nonnull String tagName) {
        var keys = tagToKeys.get(tagName);
        return keys == null || keys.isEmpty() ? Collections.emptySet() : new HashSet<>(keys);
    }

    @Nonnull
    public Set<String> allTags() {
        return tagToKeys.isEmpty() ? Collections.emptySet() : new HashSet<>(tagToKeys.keySet());
    }

    @Nonnull
    public Set<T> allKeys() {
        return keyToTags.isEmpty() ? Collections.emptySet() : new HashSet<>(keyToTags.keySet());
    }

    @Nonnull
    public Map<String, Set<T>> getAllEntries() {
        if (tagToKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Set<T>> result = new HashMap<>(tagToKeys.size());
        for (var entry : tagToKeys.object2ReferenceEntrySet()) {
            var tag = entry.getKey();
            ObjectOpenHashSet<T> keys = entry.getValue();
            if (keys != null && !keys.isEmpty()) {
                result.put(tag, new HashSet<>(keys));
            }
        }
        return result;
    }

    public boolean hasTag(@Nonnull T key, @Nonnull String tagName) {
        var tags = keyToTags.get(key);
        return tags != null && tags.contains(tagName);
    }

    public boolean hasAnyTag(@Nonnull T key, @Nonnull String... tagNames) {
        var tags = keyToTags.get(key);
        if (tags == null || tagNames.length == 0) {
            return false;
        }
        return Arrays.stream(tagNames).anyMatch(tags::contains);
    }

    public void create(@Nonnull T key, @Nonnull String tagName) {
        tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>()).add(key);
        keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
    }

    public void create(@Nonnull Set<T> keys, @Nonnull String tagName) {
        var keySet = tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>());
        keySet.addAll(keys);
        keys.forEach(key -> keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName));
    }

    public void remove(@Nonnull String tagName) {
        var keys = tagToKeys.remove(tagName);
        if (keys == null) {
            return;
        }

        keys.forEach(key -> {
            var tagsForKey = keyToTags.get(key);
            if (tagsForKey != null) {
                tagsForKey.remove(tagName);
                if (tagsForKey.isEmpty()) {
                    keyToTags.remove(key);
                }
            }
        });
    }

    public int tagCount() {
        return tagToKeys.size();
    }

    public int keyCount() {
        return keyToTags.size();
    }

    public int associationCount() {
        return tagToKeys.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    public boolean exists(@Nonnull String tagName) {
        return tagToKeys.containsKey(tagName);
    }

    public void clear() {
        tagToKeys.clear();
        keyToTags.clear();
    }
}

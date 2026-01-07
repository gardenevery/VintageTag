package com.gardenevery.vintagetag;

import java.util.Collections;
import java.util.HashMap;
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
        return tags == null || tags.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(tags);
    }

    @Nonnull
    public Set<T> getKeys(@Nonnull String tagName) {
        var keys = tagToKeys.get(tagName);
        return keys == null || keys.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(keys);
    }

    @Nonnull
    public Set<String> allTags() {
        return tagToKeys.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(tagToKeys.keySet());
    }

    @Nonnull
    public Set<T> allKeys() {
        return keyToTags.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(keyToTags.keySet());
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
            result.put(tag, Collections.unmodifiableSet(keys));
        }
        return Collections.unmodifiableMap(result);
    }

    public boolean hasTag(@Nonnull T key, @Nonnull String tagName) {
        var tags = keyToTags.get(key);
        return tags != null && tags.contains(tagName);
    }

    public boolean hasAnyTag(@Nonnull T key, @Nonnull String... tagNames) {
        var tags = keyToTags.get(key);
        if (tags == null) {
            return false;
        }

        for (var tag : tagNames) {
            if (tags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllTags(@Nonnull T key, @Nonnull String... tagNames) {
        var tags = keyToTags.get(key);
        if (tags == null || tagNames.length == 0) {
            return false;
        }

        for (var tag : tagNames) {
            if (!tags.contains(tag)) {
                return false;
            }
        }
        return true;
    }

    public void register(@Nonnull T key, @Nonnull String tagName) {
        tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>()).add(key);
        keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
    }

    public void register(@Nonnull Set<T> keys, @Nonnull String tagName) {
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

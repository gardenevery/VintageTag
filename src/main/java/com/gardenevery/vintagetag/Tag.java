package com.gardenevery.vintagetag;

import java.util.Collections;
import java.util.HashSet;
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

    public boolean hasTag(@Nonnull String tagName, @Nonnull T key) {
        var tags = keyToTags.get(key);
        return tags != null && tags.contains(tagName);
    }

    public boolean hasAnyTag(@Nonnull Set<String> tagNames, @Nonnull T key) {
        var tags = keyToTags.get(key);
        return tags != null && tagNames.stream().anyMatch(tags::contains);
    }

    public void create(@Nonnull String tagName, @Nonnull T key) {
        tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>()).add(key);
        keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
    }

    public void create(@Nonnull String tagName, @Nonnull Set<T> keys) {
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

    public int getTagKeyCount(@Nonnull String tagName) {
        var keys = tagToKeys.get(tagName);
        return keys == null ? 0 : keys.size();
    }

    public int getKeyTagCount(@Nonnull T key) {
        var tags = keyToTags.get(key);
        return tags == null ? 0 : tags.size();
    }

    public boolean exists(@Nonnull String tagName) {
        return tagToKeys.containsKey(tagName);
    }

    public boolean containsKey(@Nonnull T key) {
        return keyToTags.containsKey(key);
    }

    public void clear() {
        tagToKeys.clear();
        keyToTags.clear();
    }
}

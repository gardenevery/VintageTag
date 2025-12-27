package com.gardenevery.vintagetag;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

final class Tag<T> {

    Tag() {}

    private final Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys = new Object2ReferenceOpenHashMap<>();
    private final Object2ReferenceOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags = new Object2ReferenceOpenHashMap<>();

    @Nonnull
    public Set<String> getTag(@Nonnull T key) {
        var tags = keyToTags.get(key);

        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(tags);
    }

    @Nonnull
    public Set<T> getKey(@Nonnull String tagName) {
        var keys = tagToKeys.get(tagName);

        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(keys);
    }

    @Nonnull
    public Set<String> getAllTag() {
        return tagToKeys.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(tagToKeys.keySet());
    }

    @Nonnull
    public Set<T> getAllKey() {
        return keyToTags.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(keyToTags.keySet());
    }

    public boolean hasTag(@Nonnull String tagName, @Nonnull T key) {
        var tags = keyToTags.get(key);
        return tags != null && tags.contains(tagName);
    }

    public boolean hasAnyTag(@Nonnull Set<String> tagNames, @Nonnull T key) {
        var tags = keyToTags.get(key);
        return tags != null && tagNames.stream().anyMatch(tags::contains);
    }

    public void createTag(@Nonnull String tagName, @Nonnull T key) {
        tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>()).add(key);
        keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
    }

    public void createTag(@Nonnull String tagName, @Nonnull Set<T> keys) {
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

    public void clean() {
        tagToKeys.clear();
        keyToTags.clear();
    }

    public boolean doesTagExist(@Nonnull String tagName) {
        return tagToKeys.containsKey(tagName);
    }

    public boolean containsKey(@Nonnull T key) {
        return keyToTags.containsKey(key);
    }

    public int getTagCount() {
        return tagToKeys.size();
    }

    public int getKeyCount() {
        return keyToTags.size();
    }

    public int getAssociations() {
        return tagToKeys.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
}

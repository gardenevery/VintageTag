package com.gardenevery.vintagetag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

final class Tag<T> {
    private final ImmutableMap<String, ImmutableSet<T>> tagToKeys;
    private final ImmutableMap<T, ImmutableSet<String>> keyToTags;
    private final ImmutableSet<String> tags;
    private final int associationCount;

    public Tag() {
        this.tagToKeys = ImmutableMap.of();
        this.keyToTags = ImmutableMap.of();
        this.tags = ImmutableSet.of();
        this.associationCount = 0;
    }

    private Tag(MutableTagContainer<T> container) {
        this.tagToKeys = buildMap(container.tagToKeys);
        this.keyToTags = buildMap(container.keyToTags);
        this.tags = this.tagToKeys.keySet();

        this.associationCount = this.tagToKeys.values().stream()
                .mapToInt(ImmutableSet::size)
                .sum();
    }

    @Nonnull
    public Set<String> getTags(@Nonnull T key) {
        var tags = keyToTags.get(key);
        return tags == null ? Collections.emptySet() : tags;
    }

    @Nonnull
    public List<String> getTagsList(@Nonnull T key) {
        var tags = keyToTags.get(key);
        return tags == null ? Collections.emptyList() : new ArrayList<>(tags);
    }

    @Nonnull
    public Set<T> getKeys(@Nonnull String tagName) {
        var keys = tagToKeys.get(tagName);
        return keys == null ? Collections.emptySet() : keys;
    }

    @Nonnull
    public List<T> getKeysList(@Nonnull String tagName) {
        var keys = tagToKeys.get(tagName);
        return keys == null ? Collections.emptyList() : new ArrayList<>(keys);
    }

    @Nonnull
    public ImmutableSet<String> getAllTags() {
        return tags;
    }

    @Nonnull
    public List<String> getAllTagsList() {
        return ImmutableList.copyOf(tags);
    }

    @Nonnull
    public ImmutableSet<T> getAllKeys() {
        return keyToTags.keySet();
    }

    @Nonnull
    public List<T> getAllKeysList() {
        return ImmutableList.copyOf(keyToTags.keySet());
    }

    @Nonnull
    public ImmutableMap<String, ImmutableSet<T>> getAllEntries() {
        return tagToKeys;
    }

    public boolean hasTag(@Nonnull T key, @Nonnull String tagName) {
        var tags = keyToTags.get(key);
        return tags != null && tags.contains(tagName);
    }

    public boolean hasAnyTag(@Nonnull T key, @Nonnull String... tagNames) {
        if (tagNames.length == 0) {
            return false;
        }

        var tags = keyToTags.get(key);
        if (tags == null) {
            return false;
        }
        return Arrays.stream(tagNames).anyMatch(tags::contains);
    }

    public boolean hasAllTags(@Nonnull T key, @Nonnull String... tagNames) {
        if (tagNames.length == 0) {
            return false;
        }

        var tags = keyToTags.get(key);
        if (tags == null) {
            return false;
        }
        return Arrays.stream(tagNames).allMatch(tags::contains);
    }

    public int getTagCount() {
        return tags.size();
    }

    public int getKeyCount() {
        return keyToTags.size();
    }

    public int getAssociationCount() {
        return associationCount;
    }

    public boolean exists(@Nonnull String tagName) {
        return tagToKeys.containsKey(tagName);
    }

    private static <K, V> ImmutableMap<K, ImmutableSet<V>> buildMap(Object2ReferenceOpenHashMap<K, ObjectOpenHashSet<V>> map) {
        if (map.isEmpty()) {
            return ImmutableMap.of();
        }

        ImmutableMap.Builder<K, ImmutableSet<V>> builder = ImmutableMap.builder();
        for (var entry : map.object2ReferenceEntrySet()) {
            builder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        }
        return builder.build();
    }

    static final class MutableTagContainer<T> {
        private final Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys;
        private final Object2ReferenceOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags;

        public MutableTagContainer() {
            this.tagToKeys = new Object2ReferenceOpenHashMap<>();
            this.keyToTags = new Object2ReferenceOpenHashMap<>();
        }

        public void register(@Nonnull Set<T> keys, @Nonnull String tagName) {
            Objects.requireNonNull(keys, "keys must not be null");
            Objects.requireNonNull(tagName, "tagName must not be null");

            if (keys.isEmpty()) {
                return;
            }

            var keySet = tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>());
            keySet.addAll(keys);

            for (T key : keys) {
                keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
            }
        }

        public void replace(@Nonnull Set<T> keys, @Nonnull String tagName) {
            Objects.requireNonNull(keys, "keys must not be null");
            Objects.requireNonNull(tagName, "tagName must not be null");

            var existingKeys = tagToKeys.remove(tagName);
            if (existingKeys != null) {
                for (T key : existingKeys) {
                    var tags = keyToTags.get(key);
                    if (tags != null) {
                        tags.remove(tagName);
                        if (tags.isEmpty()) {
                            keyToTags.remove(key);
                        }
                    }
                }
            }

            if (!keys.isEmpty()) {
                var keySet = tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>());
                keySet.addAll(keys);

                for (T key : keys) {
                    keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
                }
            }
        }

        @Nonnull
        public Tag<T> build() {
            return new Tag<>(this);
        }

        public void clear() {
            tagToKeys.clear();
            keyToTags.clear();
        }
    }
}

package com.gardenevery.vintagetag;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

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

        int count = 0;
        for (ImmutableSet<T> keys : this.tagToKeys.values()) {
            count += keys.size();
        }
        this.associationCount = count;
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

    @Nonnull
    public Set<String> getTags(@Nonnull T key) {
        var tags = keyToTags.get(key);
        return tags == null ? Collections.emptySet() : tags;
    }

    @Nonnull
    public Set<T> getKeys(@Nonnull String tagName) {
        var keys = tagToKeys.get(tagName);
        return keys == null ? Collections.emptySet() : keys;
    }

    @Nonnull
    public ImmutableSet<String> getAllTags() {
        return tags;
    }

    @Nonnull
    public ImmutableSet<T> getAllKeys() {
        return keyToTags.keySet();
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
        if (tagNames.length == 0) {
            return false;
        }

        var tags = keyToTags.get(key);
        if (tags == null) {
            return false;
        }

        for (var tag : tagNames) {
            if (!tags.contains(tag)) {
                return false;
            }
        }
        return true;
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

    @SuppressWarnings("UnusedReturnValue")
    static final class MutableTagContainer<T> {
        private final Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys;
        private final Object2ReferenceOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags;

        public MutableTagContainer() {
            this.tagToKeys = new Object2ReferenceOpenHashMap<>();
            this.keyToTags = new Object2ReferenceOpenHashMap<>();
        }

        public void register(@Nonnull T key, @Nonnull String tagName) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(tagName, "tagName must not be null");

            tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>()).add(key);
            keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
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

        public void remove(@Nonnull String tagName) {
            Objects.requireNonNull(tagName, "tagName must not be null");

            var keys = tagToKeys.remove(tagName);
            if (keys != null) {
                for (T key : keys) {
                    var tags = keyToTags.get(key);
                    if (tags != null) {
                        tags.remove(tagName);
                        if (tags.isEmpty()) {
                            keyToTags.remove(key);
                        }
                    }
                }
            }
        }

        public void clear() {
            tagToKeys.clear();
            keyToTags.clear();
        }

        @Nonnull
        public Tag<T> build() {
            return new Tag<>(this);
        }
    }
}

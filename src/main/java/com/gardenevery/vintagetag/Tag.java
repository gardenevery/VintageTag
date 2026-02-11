package com.gardenevery.vintagetag;

import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

final class Tag<T extends TagEntry> {
	private final ImmutableMap<String, ImmutableSet<T>> tagToEntries;
	private final ImmutableMap<T, ImmutableSet<String>> entryToTags;
	private final ImmutableSet<String> tags;

	public Tag() {
		this.tagToEntries = ImmutableMap.of();
		this.entryToTags = ImmutableMap.of();
		this.tags = ImmutableSet.of();
	}

	private Tag(ImmutableMap<String, ImmutableSet<T>> tagToEntries, ImmutableMap<T, ImmutableSet<String>> entryToTags,
			ImmutableSet<String> tags) {
		this.tagToEntries = tagToEntries;
		this.entryToTags = entryToTags;
		this.tags = tags;
	}

	@Nonnull
	public ImmutableSet<String> getTags(@Nonnull T entry) {
		var tags = entryToTags.get(entry);
		return tags == null ? ImmutableSet.of() : tags;
	}

	@Nonnull
	public ImmutableList<String> getTagsList(@Nonnull T entry) {
		return getTags(entry).asList();
	}

	@Nonnull
	public ImmutableSet<T> getKeys(@Nonnull String tagName) {
		var keys = tagToEntries.get(tagName);
		return keys == null ? ImmutableSet.of() : keys;
	}

	@Nonnull
	public ImmutableList<T> getKeysList(@Nonnull String tagName) {
		return getKeys(tagName).asList();
	}

	@Nonnull
	public ImmutableSet<String> getAllTags() {
		return tags;
	}

	@Nonnull
	public ImmutableList<String> getAllTagsList() {
		return tags.asList();
	}

	@Nonnull
	public ImmutableSet<T> getAllKeys() {
		return entryToTags.keySet();
	}

	@Nonnull
	public ImmutableList<T> getAllKeysList() {
		return ImmutableList.copyOf(entryToTags.keySet());
	}

	@Nonnull
	public ImmutableMap<String, ImmutableSet<T>> getAllEntries() {
		return tagToEntries;
	}

	public boolean hasTag(@Nonnull T entry, @Nonnull String tagName) {
		var entriesForTag = tagToEntries.get(tagName);
		return entriesForTag != null && entriesForTag.contains(entry);
	}

	public boolean hasAnyTag(@Nonnull T entry, @Nonnull String... tagNames) {
		for (var tagName : tagNames) {
			if (hasTag(entry, tagName)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAllTags(@Nonnull T entry, @Nonnull String... tagNames) {
		for (var tagName : tagNames) {
			if (!hasTag(entry, tagName)) {
				return false;
			}
		}
		return true;
	}

	public boolean isTagged(@Nonnull T entry) {
		var tags = entryToTags.get(entry);
		return tags != null && !tags.isEmpty();
	}

	public boolean exists(@Nonnull String tagName) {
		return tags.contains(tagName);
	}

	public int getTagCount() {
		return tags.size();
	}

	public int getKeyCount() {
		return entryToTags.size();
	}

	static final class MutableTagContainer<T extends TagEntry> {
		private final Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> tagToEntries;
		private final Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> entryToTags;

		public MutableTagContainer() {
			this.tagToEntries = new Object2ObjectOpenHashMap<>();
			this.entryToTags = new Object2ObjectOpenHashMap<>();
		}

		public void register(@Nonnull Set<T> entries, @Nonnull String tagName) {
			if (entries.isEmpty()) {
				return;
			}

			var entrySet = tagToEntries.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>(entries.size()));

			for (T entry : entries) {
				entrySet.add(entry);
				entryToTags.computeIfAbsent(entry, k -> new ObjectOpenHashSet<>(4)).add(tagName);
			}
		}

		public void replace(@Nonnull Set<T> entries, @Nonnull String tagName) {
			var existingEntries = tagToEntries.remove(tagName);
			if (existingEntries != null) {
				for (T entry : existingEntries) {
					var tags = entryToTags.get(entry);
					if (tags != null) {
						tags.remove(tagName);
						if (tags.isEmpty()) {
							entryToTags.remove(entry);
						}
					}
				}
			}

			register(entries, tagName);
		}

		@Nonnull
		public Tag<T> build() {
			ImmutableMap.Builder<String, ImmutableSet<T>> tagToEntriesBuilder = ImmutableMap.builder();
			ImmutableSet.Builder<String> allTagsBuilder = ImmutableSet.builder();

			for (var entry : tagToEntries.entrySet()) {
				var entrySet = entry.getValue();
				if (!entrySet.isEmpty()) {
					tagToEntriesBuilder.put(entry.getKey(), ImmutableSet.copyOf(entrySet));
					allTagsBuilder.add(entry.getKey());
				}
			}

			ImmutableMap.Builder<T, ImmutableSet<String>> entryToTagsBuilder = ImmutableMap.builder();

			for (var entry : entryToTags.entrySet()) {
				var tags = entry.getValue();
				if (!tags.isEmpty()) {
					entryToTagsBuilder.put(entry.getKey(), ImmutableSet.copyOf(tags));
				}
			}

			return new Tag<>(tagToEntriesBuilder.build(), entryToTagsBuilder.build(), allTagsBuilder.build());
		}

		private static <K, V> void copyMap(Object2ObjectOpenHashMap<K, ObjectOpenHashSet<V>> source,
				Object2ObjectOpenHashMap<K, ObjectOpenHashSet<V>> target, Predicate<ObjectOpenHashSet<V>> filter) {

			for (var entry : source.object2ObjectEntrySet()) {
				var value = entry.getValue();
				if (filter.test(value)) {
					target.put(entry.getKey(), new ObjectOpenHashSet<>(value));
				}
			}
		}

		public void copyTo(MutableTagContainer<T> other) {
			copyMap(this.tagToEntries, other.tagToEntries, set -> !set.isEmpty());
			copyMap(this.entryToTags, other.entryToTags, set -> true);
		}

		public void clear() {
			tagToEntries.clear();
			entryToTags.clear();
		}
	}
}

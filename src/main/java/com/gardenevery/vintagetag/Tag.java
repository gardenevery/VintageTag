package com.gardenevery.vintagetag;

import java.util.Set;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

final class Tag<T extends TagEntry> {
	private final ImmutableMap<String, ImmutableSet<T>> tagToEntries;
	private final ImmutableMap<T, ImmutableSet<String>> entryToTags;

	public Tag() {
		this.tagToEntries = ImmutableMap.of();
		this.entryToTags = ImmutableMap.of();
	}

	private Tag(ImmutableMap<String, ImmutableSet<T>> tagToEntries, ImmutableMap<T, ImmutableSet<String>> entryToTags) {
		this.tagToEntries = tagToEntries;
		this.entryToTags = entryToTags;
	}

	@Nonnull
	public ImmutableSet<String> getTags(@Nonnull T entry) {
		return entryToTags.getOrDefault(entry, ImmutableSet.of());
	}

	@Nonnull
	public ImmutableSet<T> getKeys(@Nonnull String tagName) {
		return tagToEntries.getOrDefault(tagName, ImmutableSet.of());
	}

	@Nonnull
	public ImmutableList<String> getTagsList(@Nonnull T entry) {
		return getTags(entry).asList();
	}

	@Nonnull
	public ImmutableList<T> getKeysList(@Nonnull String tagName) {
		return getKeys(tagName).asList();
	}

	@Nonnull
	public ImmutableSet<String> getAllTags() {
		return tagToEntries.keySet();
	}

	@Nonnull
	public ImmutableList<String> getAllTagsList() {
		return getAllTags().asList();
	}

	@Nonnull
	public ImmutableSet<T> getAllKeys() {
		return entryToTags.keySet();
	}

	@Nonnull
	public ImmutableList<T> getAllKeysList() {
		return getAllKeys().asList();
	}

	@Nonnull
	public ImmutableMap<String, ImmutableSet<T>> getAllEntries() {
		return tagToEntries;
	}

	public boolean hasTag(@Nonnull T entry, @Nonnull String tagName) {
		return getTags(entry).contains(tagName);
	}

	public boolean hasAnyTag(@Nonnull T entry, @Nonnull String... tagNames) {
		var tags = getTags(entry);
		for (var tag : tagNames) {
			if (tags.contains(tag)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAllTags(@Nonnull T entry, @Nonnull String... tagNames) {
		var tags = getTags(entry);
		for (var tag : tagNames) {
			if (!tags.contains(tag)) {
				return false;
			}
		}
		return true;
	}

	public boolean isTagged(@Nonnull T entry) {
		return !getTags(entry).isEmpty();
	}

	public boolean exists(@Nonnull String tagName) {
		return tagToEntries.containsKey(tagName);
	}

	public int getTagCount() {
		return tagToEntries.size();
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

			var entrySet = tagToEntries.computeIfAbsent(tagName,
					k -> new ObjectOpenHashSet<>(Math.max(entries.size(), 4)));

			for (T entry : entries) {
				if (entry.isEmpty()) {
					continue;
				}

				if (entrySet.add(entry)) {
					entryToTags.computeIfAbsent(entry, k -> new ObjectOpenHashSet<>(4)).add(tagName);
				}
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
			var expandedCache = new Object2ObjectOpenHashMap<String, ImmutableSet<T>>();
			var tempExpandedEntryToTags = new Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>>();
			var processingSet = new ObjectOpenHashSet<String>();

			var expandedTagToEntriesBuilder = ImmutableMap.<String, ImmutableSet<T>>builder();

			for (var tagName : tagToEntries.keySet()) {
				ImmutableSet<T> expandedKeys = expandTag(tagName, processingSet, expandedCache);
				processingSet.clear();
				expandedTagToEntriesBuilder.put(tagName, expandedKeys);

				for (T key : expandedKeys) {
					if (!key.isTag()) {
						tempExpandedEntryToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
					}
				}
			}

			var expandedEntryToTagsBuilder = ImmutableMap.<T, ImmutableSet<String>>builder();
			for (var entry : tempExpandedEntryToTags.entrySet()) {
				expandedEntryToTagsBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
			}

			return new Tag<>(expandedTagToEntriesBuilder.build(), expandedEntryToTagsBuilder.build());
		}

		private ImmutableSet<T> expandTag(String tagName, Set<String> processing,
				Object2ObjectOpenHashMap<String, ImmutableSet<T>> cache) {
			var cached = cache.get(tagName);
			if (cached != null) {
				return cached;
			}

			if (!processing.add(tagName)) {
				return ImmutableSet.of();
			}

			try {
				var result = new ObjectOpenHashSet<T>();
				var entries = tagToEntries.get(tagName);
				if (entries != null) {
					for (T entry : entries) {
						if (entry.isEmpty()) {
							continue;
						}

						if (entry.isTag()) {
							result.addAll(expandTag(entry.getTagName(), processing, cache));
						} else {
							result.add(entry);
						}
					}
				}
				var expanded = ImmutableSet.copyOf(result);
				cache.put(tagName, expanded);
				return expanded;
			} finally {
				processing.remove(tagName);
			}
		}

		public void clear() {
			tagToEntries.clear();
			entryToTags.clear();
		}
	}
}

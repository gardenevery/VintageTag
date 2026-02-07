package com.gardenevery.vintagetag;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

final class Tag<T extends TagEntry> {
	private final ImmutableMap<String, ImmutableSet<T>> tagToEntries;
	private final ImmutableMap<T, ImmutableSet<String>> entryToDirectTags;
	private final ImmutableSet<String> tags;

	private final Map<String, ImmutableSet<T>> expansionCache = new ConcurrentHashMap<>();
	private final ThreadLocal<Set<String>> expansionStack = ThreadLocal.withInitial(HashSet::new);

	public Tag() {
		this.tagToEntries = ImmutableMap.of();
		this.entryToDirectTags = ImmutableMap.of();
		this.tags = ImmutableSet.of();
	}

	private Tag(ImmutableMap<String, ImmutableSet<T>> tagToEntries,
			ImmutableMap<T, ImmutableSet<String>> entryToDirectTags, ImmutableSet<String> tags) {
		this.tagToEntries = tagToEntries;
		this.entryToDirectTags = entryToDirectTags;
		this.tags = tags;
	}

	@Nonnull
	public ImmutableSet<T> getKeys(@Nonnull String tagName) {
		if (!exists(tagName)) {
			return ImmutableSet.of();
		}
		return expansionCache.computeIfAbsent(tagName, this::expandTag);
	}

	@Nonnull
	public ImmutableList<T> getKeysList(@Nonnull String tagName) {
		return getKeys(tagName).asList();
	}

	@Nonnull
	public ImmutableSet<String> getTags(@Nonnull T entry) {
		var tags = entryToDirectTags.get(entry);
		return tags == null ? ImmutableSet.of() : tags;
	}

	@Nonnull
	public ImmutableList<String> getTagsList(@Nonnull T entry) {
		return getTags(entry).asList();
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
		return entryToDirectTags.keySet();
	}

	@Nonnull
	public ImmutableList<T> getAllKeysList() {
		return ImmutableList.copyOf(entryToDirectTags.keySet());
	}

	@Nonnull
	public ImmutableMap<String, ImmutableSet<T>> getAllEntries() {
		return tagToEntries;
	}

	public boolean hasTag(@Nonnull T entry, @Nonnull String tagName) {
		var directTags = entryToDirectTags.get(entry);
		if (directTags != null && directTags.contains(tagName)) {
			return true;
		}
		return getKeys(tagName).contains(entry);
	}

	public boolean hasAnyTag(@Nonnull T entry, @Nonnull String... tagNames) {
		var directTags = entryToDirectTags.get(entry);
		if (directTags != null) {
			for (var tagName : tagNames) {
				if (directTags.contains(tagName) || getKeys(tagName).contains(entry)) {
					return true;
				}
			}
		} else {
			for (var tagName : tagNames) {
				if (getKeys(tagName).contains(entry)) {
					return true;
				}
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
		var tags = entryToDirectTags.get(entry);
		return tags != null && !tags.isEmpty();
	}

	public boolean exists(@Nonnull String tagName) {
		return tags.contains(tagName);
	}

	public int getTagCount() {
		return tags.size();
	}

	public int getKeyCount() {
		return entryToDirectTags.size();
	}

	public int getAssociationCount() {
		int count = 0;
		for (var entries : tagToEntries.values()) {
			count += entries.size();
		}
		return count;
	}

	@Nonnull
	private ImmutableSet<T> expandTag(@Nonnull String tagName) {
		ImmutableSet.Builder<T> result = ImmutableSet.builder();
		var visited = expansionStack.get();

		try {
			if (visited.contains(tagName)) {
				return ImmutableSet.of();
			}

			visited.add(tagName);
			expandTagRecursive(tagName, result, visited);
		} finally {
			visited.remove(tagName);
			if (visited.isEmpty()) {
				expansionStack.remove();
			}
		}
		return result.build();
	}

	private void expandTagRecursive(@Nonnull String tagName, ImmutableSet.Builder<T> result, Set<String> visited) {
		var entries = tagToEntries.get(tagName);
		if (entries == null) {
			return;
		}

		for (T entry : entries) {
			if (entry.isTag()) {
				var referencedTag = entry.getTagName();
				if (referencedTag != null && !visited.contains(referencedTag)) {
					visited.add(referencedTag);
					expandTagRecursive(referencedTag, result, visited);
					visited.remove(referencedTag);
				}
			} else {
				result.add(entry);
			}
		}
	}

	static final class MutableTagContainer<T extends TagEntry> {
		private final Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> tagToEntries;
		private final Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> entryToDirectTags;

		public MutableTagContainer() {
			this.tagToEntries = new Object2ObjectOpenHashMap<>();
			this.entryToDirectTags = new Object2ObjectOpenHashMap<>();
		}

		public void register(@Nonnull Set<T> entries, @Nonnull String tagName) {
			if (entries.isEmpty()) {
				return;
			}

			var entrySet = tagToEntries.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>(entries.size()));

			for (T entry : entries) {
				entrySet.add(entry);

				if (!entry.isTag()) {
					entryToDirectTags.computeIfAbsent(entry, k -> new ObjectOpenHashSet<>(4)).add(tagName);
				}
			}
		}

		public void replace(@Nonnull Set<T> entries, @Nonnull String tagName) {
			var existingEntries = tagToEntries.remove(tagName);
			if (existingEntries != null) {
				for (T entry : existingEntries) {
					if (!entry.isTag()) {
						var tags = entryToDirectTags.get(entry);
						if (tags != null) {
							tags.remove(tagName);
							if (tags.isEmpty()) {
								entryToDirectTags.remove(entry);
							}
						}
					}
				}
			}

			register(entries, tagName);
		}

		@Nonnull
		public Tag<T> build() {
			ImmutableMap.Builder<String, ImmutableSet<T>> tagToEntriesBuilder = ImmutableMap.builder();
			ImmutableMap.Builder<T, ImmutableSet<String>> entryToTagsBuilder = ImmutableMap.builder();
			ImmutableSet.Builder<String> allTagsBuilder = ImmutableSet.builder();

			for (var entry : tagToEntries.object2ObjectEntrySet()) {
				var entrySet = entry.getValue();
				if (!entrySet.isEmpty()) {
					tagToEntriesBuilder.put(entry.getKey(), ImmutableSet.copyOf(entrySet));
					allTagsBuilder.add(entry.getKey());
				}
			}

			for (var entry : entryToDirectTags.object2ObjectEntrySet()) {
				entryToTagsBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
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
			copyMap(this.entryToDirectTags, other.entryToDirectTags, set -> true);
		}

		public void clear() {
			tagToEntries.clear();
			entryToDirectTags.clear();
		}
	}
}

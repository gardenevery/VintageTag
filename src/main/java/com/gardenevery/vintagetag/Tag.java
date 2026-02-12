package com.gardenevery.vintagetag;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
	private final ImmutableSet<T> keys;

	private final Map<String, ImmutableSet<T>> expandedTagToEntries = new ConcurrentHashMap<>();
	private final Map<T, ImmutableSet<String>> expandedEntryToTags = new ConcurrentHashMap<>();
	private final Object lock = new Object();

	public Tag() {
		this.tagToEntries = ImmutableMap.of();
		this.entryToTags = ImmutableMap.of();
		this.tags = ImmutableSet.of();
		this.keys = ImmutableSet.of();
	}

	private Tag(ImmutableMap<String, ImmutableSet<T>> tagToEntries, ImmutableMap<T, ImmutableSet<String>> entryToTags,
			ImmutableSet<String> tags, ImmutableSet<T> keys) {
		this.tagToEntries = tagToEntries;
		this.entryToTags = entryToTags;
		this.tags = tags;
		this.keys = keys;
	}

	@Nonnull
	public ImmutableSet<String> getTags(@Nonnull T entry) {
		if (!entry.isKey()) {
			return ImmutableSet.of();
		}

		var cached = expandedEntryToTags.get(entry);
		if (cached != null) {
			return cached;
		}

		synchronized (lock) {
			cached = expandedEntryToTags.get(entry);
			if (cached == null) {
				for (String tag : tags) {
					getKeys(tag);
				}
				cached = expandedEntryToTags.get(entry);
				if (cached == null) {
					cached = ImmutableSet.of();
				}
			}
		}
		return cached;
	}

	@Nonnull
	public ImmutableSet<T> getKeys(@Nonnull String tagName) {
		var cached = expandedTagToEntries.get(tagName);
		if (cached != null) {
			return cached;
		}

		synchronized (lock) {
			cached = expandedTagToEntries.get(tagName);
			if (cached == null) {
				Set<String> processing = new ObjectOpenHashSet<>();
				var expanded = expandTag(tagName, processing);

				expandedTagToEntries.put(tagName, expanded);

				for (T key : expanded) {
					if (key.isKey()) {
						expandedEntryToTags.merge(key, ImmutableSet.of(tagName),
								(old, add) -> ImmutableSet.<String>builder().addAll(old).addAll(add).build());
					}
				}
				cached = expanded;
			}
		}
		return cached;
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
		return tags;
	}

	@Nonnull
	public ImmutableList<String> getAllTagsList() {
		return tags.asList();
	}

	@Nonnull
	public ImmutableSet<T> getAllKeys() {
		return keys;
	}

	@Nonnull
	public ImmutableList<T> getAllKeysList() {
		return keys.asList();
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
		return tags.contains(tagName);
	}

	public int getTagCount() {
		return tags.size();
	}

	public int getKeyCount() {
		return keys.size(); // 修改为keys字段的size
	}

	private ImmutableSet<T> expandTag(String tagName, Set<String> processing) {
		if (!processing.add(tagName)) {
			return ImmutableSet.of();
		}

		try {
			ObjectOpenHashSet<T> result = new ObjectOpenHashSet<>();
			var entries = tagToEntries.get(tagName);
			if (entries != null) {
				for (T entry : entries) {
					var include = entry.asTagInclude();
					if (include != null) {
						result.addAll(expandTag(include.getTagName(), processing));
					} else {
						result.add(entry);
					}
				}
			}
			return ImmutableSet.copyOf(result);
		} finally {
			processing.remove(tagName);
		}
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
			ImmutableSet.Builder<T> allKeysBuilder = ImmutableSet.builder();

			for (var entry : tagToEntries.entrySet()) {
				var entrySet = entry.getValue();
				if (!entrySet.isEmpty()) {
					tagToEntriesBuilder.put(entry.getKey(), ImmutableSet.copyOf(entrySet));
					allTagsBuilder.add(entry.getKey());
				}
			}

			ImmutableMap.Builder<T, ImmutableSet<String>> entryToTagsBuilder = ImmutableMap.builder();

			for (var entry : entryToTags.entrySet()) {
				T key = entry.getKey();
				if (key.isKey()) {
					var tags = entry.getValue();
					if (!tags.isEmpty()) {
						entryToTagsBuilder.put(key, ImmutableSet.copyOf(tags));
						allKeysBuilder.add(key);
					}
				}
			}

			return new Tag<>(tagToEntriesBuilder.build(), entryToTagsBuilder.build(), allTagsBuilder.build(),
					allKeysBuilder.build());
		}

		public void clear() {
			tagToEntries.clear();
			entryToTags.clear();
		}
	}
}

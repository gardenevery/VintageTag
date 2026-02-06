package com.gardenevery.vintagetag;

import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

final class Tag<T> {
	private final ImmutableMap<String, ImmutableSet<T>> tagToKeys;
	private final ImmutableMap<T, ImmutableSet<String>> keyToTags;
	private final ImmutableSet<String> tags;
	private Integer associationCount = null;

	public Tag() {
		this.tagToKeys = ImmutableMap.of();
		this.keyToTags = ImmutableMap.of();
		this.tags = ImmutableSet.of();
	}

	private Tag(ImmutableMap<String, ImmutableSet<T>> tagToKeys, ImmutableMap<T, ImmutableSet<String>> keyToTags,
			ImmutableSet<String> tags) {
		this.tagToKeys = tagToKeys;
		this.keyToTags = keyToTags;
		this.tags = tags;
	}

	@Nonnull
	public ImmutableSet<String> getTags(@Nonnull T key) {
		var tags = keyToTags.get(key);
		return tags == null ? ImmutableSet.of() : tags;
	}

	@Nonnull
	public ImmutableList<String> getTagsList(@Nonnull T key) {
		var tags = keyToTags.get(key);
		return tags == null ? ImmutableList.of() : ImmutableList.copyOf(tags);
	}

	@Nonnull
	public ImmutableSet<T> getKeys(@Nonnull String tagName) {
		var keys = tagToKeys.get(tagName);
		return keys == null ? ImmutableSet.of() : keys;
	}

	@Nonnull
	public ImmutableList<T> getKeysList(@Nonnull String tagName) {
		var keys = tagToKeys.get(tagName);
		return keys == null ? ImmutableList.of() : ImmutableList.copyOf(keys);
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
		return keyToTags.keySet();
	}

	@Nonnull
	public ImmutableList<T> getAllKeysList() {
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
		var tags = keyToTags.get(key);
		if (tags == null) {
			return false;
		}

		for (var tagName : tagNames) {
			if (tags.contains(tagName)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAllTags(@Nonnull T key, @Nonnull String... tagNames) {
		var tags = keyToTags.get(key);
		if (tags == null) {
			return false;
		}

		for (var tagName : tagNames) {
			if (!tags.contains(tagName)) {
				return false;
			}
		}
		return true;
	}

	public boolean isTagged(@Nonnull T key) {
		var tags = keyToTags.get(key);
		return tags != null && !tags.isEmpty();
	}

	public boolean exists(@Nonnull String tagName) {
		return tags.contains(tagName);
	}

	public int getTagCount() {
		return tags.size();
	}

	public int getKeyCount() {
		return keyToTags.size();
	}

	public int getAssociationCount() {
		var count = this.associationCount;
		if (count == null) {
			count = 0;
			for (var keys : tagToKeys.values()) {
				count += keys.size();
			}
			this.associationCount = count;
		}
		return count;
	}

	private abstract static class MapBuilder<T> {
		abstract void processTagEntry(String tag, ObjectOpenHashSet<T> keys);
		abstract void processKeyEntry(T key, ObjectOpenHashSet<String> tags);
		abstract void collectTag(String tag);

		MutableTagContainer.ExpandedMaps<T> buildFrom(Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys,
				Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags) {

			for (var entry : tagToKeys.object2ObjectEntrySet()) {
				var keySet = entry.getValue();
				if (!keySet.isEmpty()) {
					processTagEntry(entry.getKey(), keySet);
				}
				collectTag(entry.getKey());
			}

			for (var entry : keyToTags.object2ObjectEntrySet()) {
				processKeyEntry(entry.getKey(), entry.getValue());
			}

			return buildResult();
		}

		abstract MutableTagContainer.ExpandedMaps<T> buildResult();
	}

	private static class ImmutableMapBuilder<T> extends MapBuilder<T> {
		private final ImmutableMap.Builder<String, ImmutableSet<T>> tagToKeysBuilder = ImmutableMap.builder();
		private final ImmutableMap.Builder<T, ImmutableSet<String>> keyToTagsBuilder = ImmutableMap.builder();
		private final ImmutableSet.Builder<String> allTagsBuilder = ImmutableSet.builder();

		@Override
		void processTagEntry(String tag, ObjectOpenHashSet<T> keys) {
			tagToKeysBuilder.put(tag, ImmutableSet.copyOf(keys));
		}

		@Override
		void processKeyEntry(T key, ObjectOpenHashSet<String> tags) {
			keyToTagsBuilder.put(key, ImmutableSet.copyOf(tags));
		}

		@Override
		void collectTag(String tag) {
			allTagsBuilder.add(tag);
		}

		@Override
		MutableTagContainer.ExpandedMaps<T> buildResult() {
			return new MutableTagContainer.ExpandedMaps<>(tagToKeysBuilder.build(), keyToTagsBuilder.build(),
					allTagsBuilder.build());
		}
	}

	static final class MutableTagContainer<T> {
		private final Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys;
		private final Object2ObjectOpenHashMap<String, ObjectOpenHashSet<String>> tagInclusions;
		private final Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags;

		private boolean needsExpansion = false;

		public MutableTagContainer() {
			this.tagToKeys = new Object2ObjectOpenHashMap<>();
			this.tagInclusions = new Object2ObjectOpenHashMap<>();
			this.keyToTags = new Object2ObjectOpenHashMap<>();
		}

		public void register(@Nonnull Set<T> keys, @Nonnull String tagName) {
			if (!keys.isEmpty()) {
				var keySet = tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>(keys.size()));
				keySet.addAll(keys);

				for (T key : keys) {
					keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>(4)).add(tagName);
				}
			}
		}

		public void register(@Nonnull Set<T> keys, @Nonnull String tagName, @Nonnull Set<String> tagInclude) {
			if (!keys.isEmpty()) {
				var keySet = tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>(keys.size()));
				keySet.addAll(keys);

				for (T key : keys) {
					keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>(4)).add(tagName);
				}
			}

			if (!tagInclude.isEmpty()) {
				var tagSet = tagInclusions.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>(tagInclude.size()));
				tagSet.addAll(tagInclude);
				needsExpansion = true;
			}
		}

		public void replace(@Nonnull Set<T> keys, @Nonnull String tagName, @Nonnull Set<String> tagInclude) {
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

			tagInclusions.remove(tagName);
			register(keys, tagName, tagInclude);
		}

		@Nonnull
		public Tag<T> build() {
			if (needsExpansion) {
				var expanded = expandMaps();
				return new Tag<>(expanded.tagToKeys, expanded.keyToTags, expanded.allTags);
			} else {
				return buildDirect();
			}
		}

		private Tag<T> buildDirect() {
			var result = buildMapsFrom(tagToKeys, keyToTags);
			return new Tag<>(result.tagToKeys(), result.keyToTags(), result.allTags());
		}

		private ExpandedMaps<T> buildDirectMaps() {
			return buildMapsFrom(tagToKeys, keyToTags);
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

		private ExpandedMaps<T> buildMapsFrom(Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys,
				Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags) {

			return new ImmutableMapBuilder<T>().buildFrom(tagToKeys, keyToTags);
		}

		public void copyTo(MutableTagContainer<T> other) {
			copyMap(this.tagToKeys, other.tagToKeys, set -> !set.isEmpty());
			copyMap(this.keyToTags, other.keyToTags, set -> true);
			copyMap(this.tagInclusions, other.tagInclusions, set -> !set.isEmpty());
			other.needsExpansion = this.needsExpansion;
		}

		public void clear() {
			tagToKeys.clear();
			tagInclusions.clear();
			keyToTags.clear();
			needsExpansion = false;
		}

		private ExpandedMaps<T> expandMaps() {
			if (tagInclusions.isEmpty()) {
				return buildDirectMaps();
			}

			var workingMaps = createWorkingMaps();
			processInclusionsOptimized(workingMaps.tagToKeys, workingMaps.keyToTags);
			return buildMapsFrom(workingMaps.tagToKeys, workingMaps.keyToTags);
		}

		private WorkingMaps<T> createWorkingMaps() {
			Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> workingTagToKeys = new Object2ObjectOpenHashMap<>(
					Math.max(tagToKeys.size() + tagInclusions.size(), 16));
			Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> workingKeyToTags = new Object2ObjectOpenHashMap<>(
					Math.max(keyToTags.size(), 16));

			copyToWorkingMaps(workingTagToKeys, workingKeyToTags);

			for (var tag : tagInclusions.keySet()) {
				workingTagToKeys.putIfAbsent(tag, new ObjectOpenHashSet<>());
			}

			return new WorkingMaps<>(workingTagToKeys, workingKeyToTags);
		}

		private void copyToWorkingMaps(Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> workingTagToKeys,
				Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> workingKeyToTags) {

			copyMap(this.tagToKeys, workingTagToKeys, set -> !set.isEmpty());

			for (var entry : this.keyToTags.object2ObjectEntrySet()) {
				workingKeyToTags.put(entry.getKey(), new ObjectOpenHashSet<>(entry.getValue()));
			}
		}

		private void processInclusionsOptimized(Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> workingTagToKeys,
				Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> workingKeyToTags) {

			Object2IntOpenHashMap<String> inDegree = new Object2IntOpenHashMap<>(workingTagToKeys.size());
			inDegree.defaultReturnValue(0);

			Object2ObjectOpenHashMap<String, ObjectOpenHashSet<String>> adjacency = new Object2ObjectOpenHashMap<>(
					workingTagToKeys.size());

			for (var tag : workingTagToKeys.keySet()) {
				inDegree.put(tag, 0);
				adjacency.put(tag, new ObjectOpenHashSet<>(4));
			}

			for (var entry : tagInclusions.object2ObjectEntrySet()) {
				var tag = entry.getKey();
				var dependencies = entry.getValue();

				if (!adjacency.containsKey(tag)) {
					continue;
				}

				for (var dep : dependencies) {
					if (adjacency.containsKey(dep)) {
						adjacency.get(dep).add(tag);
						inDegree.addTo(tag, 1);
					}
				}
			}

			ObjectArrayList<String> topologicalOrder = new ObjectArrayList<>(workingTagToKeys.size());
			ObjectArrayFIFOQueue<String> queue = new ObjectArrayFIFOQueue<>();

			for (var entry : inDegree.object2IntEntrySet()) {
				if (entry.getIntValue() == 0) {
					queue.enqueue(entry.getKey());
				}
			}

			while (!queue.isEmpty()) {
				var current = queue.dequeue();
				topologicalOrder.add(current);

				var neighbors = adjacency.get(current);
				if (neighbors != null) {
					for (var neighbor : neighbors) {
						int newDegree = inDegree.getInt(neighbor) - 1;
						inDegree.put(neighbor, newDegree);
						if (newDegree == 0) {
							queue.enqueue(neighbor);
						}
					}
				}
			}

			for (var tag : topologicalOrder) {
				var dependencies = tagInclusions.get(tag);
				if (dependencies != null && !dependencies.isEmpty()) {
					var currentKeys = workingTagToKeys.get(tag);
					if (currentKeys == null) {
						currentKeys = new ObjectOpenHashSet<>();
						workingTagToKeys.put(tag, currentKeys);
					}

					for (var dep : dependencies) {
						var depKeys = workingTagToKeys.get(dep);
						if (depKeys != null && !depKeys.isEmpty()) {
							for (T key : depKeys) {
								if (currentKeys.add(key)) {
									workingKeyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>(4)).add(tag);
								}
							}
						}
					}
				}
			}
		}

		@Desugar
		private record WorkingMaps<T>(Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys,
				Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags) {
		}

		@Desugar
		private record ExpandedMaps<T>(ImmutableMap<String, ImmutableSet<T>> tagToKeys,
				ImmutableMap<T, ImmutableSet<String>> keyToTags, ImmutableSet<String> allTags) {
		}
	}
}

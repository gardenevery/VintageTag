package com.gardenevery.vintagetag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import com.google.common.collect.Maps;
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
		this.associationCount = null;
	}

	private Tag(ImmutableMap<String, ImmutableSet<T>> tagToKeys, ImmutableMap<T, ImmutableSet<String>> keyToTags,
			ImmutableSet<String> tags) {
		this.tagToKeys = tagToKeys;
		this.keyToTags = keyToTags;
		this.tags = tags;
	}

	@Nonnull
	public Set<String> getTags(@Nonnull T key) {
		var tags = keyToTags.get(key);
		return tags == null ? ImmutableSet.of() : tags;
	}

	@Nonnull
	public List<String> getTagsList(@Nonnull T key) {
		var tags = keyToTags.get(key);
		return tags == null ? ImmutableList.of() : ImmutableList.copyOf(tags);
	}

	@Nonnull
	public Set<T> getKeys(@Nonnull String tagName) {
		var keys = tagToKeys.get(tagName);
		return keys == null ? ImmutableSet.of() : keys;
	}

	@Nonnull
	public List<T> getKeysList(@Nonnull String tagName) {
		var keys = tagToKeys.get(tagName);
		return keys == null ? ImmutableList.of() : ImmutableList.copyOf(keys);
	}

	@Nonnull
	public ImmutableSet<String> getAllTags() {
		return tags;
	}

	@Nonnull
	public List<String> getAllTagsList() {
		return tags.asList();
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
	public Map<String, Set<T>> getAllEntries() {
		return Maps.transformValues(tagToKeys, set -> set);
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

	static final class MutableTagContainer<T> {
		private final Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys;
		private final Object2ObjectOpenHashMap<String, ObjectOpenHashSet<String>> tagInclusions;
		private final Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags;

		public MutableTagContainer() {
			this.tagToKeys = new Object2ObjectOpenHashMap<>();
			this.tagInclusions = new Object2ObjectOpenHashMap<>();
			this.keyToTags = new Object2ObjectOpenHashMap<>();
		}

		public void register(@Nonnull Set<T> keys, @Nonnull String tagName) {
			if (!keys.isEmpty()) {
				var keySet = tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>());
				keySet.addAll(keys);

				for (T key : keys) {
					keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
				}
			}
		}

		public void register(@Nonnull Set<T> keys, @Nonnull String tagName, @Nonnull Set<String> tagInclude) {
			if (!keys.isEmpty()) {
				var keySet = tagToKeys.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>());
				keySet.addAll(keys);

				for (T key : keys) {
					keyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tagName);
				}
			}

			if (!tagInclude.isEmpty()) {
				var tagSet = tagInclusions.computeIfAbsent(tagName, k -> new ObjectOpenHashSet<>());
				tagSet.addAll(tagInclude);
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
			var expanded = expandMaps();
			return new Tag<>(expanded.tagToKeys, expanded.keyToTags, expanded.allTags);
		}

		public void clear() {
			tagToKeys.clear();
			tagInclusions.clear();
			keyToTags.clear();
		}

		private ExpandedMaps<T> expandMaps() {
			if (tagInclusions.isEmpty()) {
				return buildDirectMaps();
			}

			Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> workingTagToKeys = new Object2ObjectOpenHashMap<>(
					tagToKeys.size() + tagInclusions.size());
			Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> workingKeyToTags = new Object2ObjectOpenHashMap<>(
					keyToTags.size());

			for (var entry : tagToKeys.object2ObjectEntrySet()) {
				var tag = entry.getKey();
				var keys = entry.getValue();
				workingTagToKeys.put(tag, new ObjectOpenHashSet<>(keys));

				for (T key : keys) {
					workingKeyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tag);
				}
			}

			for (var tag : tagInclusions.keySet()) {
				workingTagToKeys.putIfAbsent(tag, new ObjectOpenHashSet<>());
			}

			processInclusions(workingTagToKeys, workingKeyToTags);
			return build(workingTagToKeys, workingKeyToTags);
		}

		private ExpandedMaps<T> buildDirectMaps() {
			ObjectOpenHashSet<String> allTags = new ObjectOpenHashSet<>();
			allTags.addAll(tagToKeys.keySet());

			ImmutableMap.Builder<String, ImmutableSet<T>> tagToKeysBuilder = ImmutableMap.builder();
			ImmutableMap.Builder<T, ImmutableSet<String>> keyToTagsBuilder = ImmutableMap.builder();

			for (var entry : tagToKeys.object2ObjectEntrySet()) {
				if (!entry.getValue().isEmpty()) {
					tagToKeysBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
				}
			}

			for (var entry : keyToTags.object2ObjectEntrySet()) {
				keyToTagsBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
			}

			return new ExpandedMaps<>(tagToKeysBuilder.build(), keyToTagsBuilder.build(), ImmutableSet.copyOf(allTags));
		}

		private void processInclusions(Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> workingTagToKeys,
				Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> workingKeyToTags) {

			Object2IntOpenHashMap<String> inDegree = new Object2IntOpenHashMap<>();
			inDegree.defaultReturnValue(0);

			Object2ObjectOpenHashMap<String, ObjectOpenHashSet<String>> adjacency = new Object2ObjectOpenHashMap<>();

			for (var tag : workingTagToKeys.keySet()) {
				inDegree.put(tag, 0);
				adjacency.put(tag, new ObjectOpenHashSet<>());
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

			ObjectArrayFIFOQueue<String> queue = new ObjectArrayFIFOQueue<>();
			for (var entry : inDegree.object2IntEntrySet()) {
				if (entry.getIntValue() == 0) {
					queue.enqueue(entry.getKey());
				}
			}

			ObjectArrayList<String> topologicalOrder = new ObjectArrayList<>();
			while (!queue.isEmpty()) {
				var current = queue.dequeue();
				topologicalOrder.add(current);

				for (var neighbor : adjacency.get(current)) {
					int newDegree = inDegree.getInt(neighbor) - 1;
					inDegree.put(neighbor, newDegree);
					if (newDegree == 0) {
						queue.enqueue(neighbor);
					}
				}
			}

			for (var tag : topologicalOrder) {
				var dependencies = tagInclusions.get(tag);
				if (dependencies != null) {
					var currentKeys = workingTagToKeys.get(tag);

					for (var dep : dependencies) {
						var depKeys = workingTagToKeys.get(dep);
						if (depKeys != null && !depKeys.isEmpty()) {
							for (T key : depKeys) {
								if (currentKeys.add(key)) {
									workingKeyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tag);
								}
							}
						}
					}
				}
			}
		}

		private ExpandedMaps<T> build(Object2ObjectOpenHashMap<String, ObjectOpenHashSet<T>> workingTagToKeys,
				Object2ObjectOpenHashMap<T, ObjectOpenHashSet<String>> workingKeyToTags) {

			ImmutableMap.Builder<String, ImmutableSet<T>> tagToKeysBuilder = ImmutableMap.builder();
			ImmutableMap.Builder<T, ImmutableSet<String>> keyToTagsBuilder = ImmutableMap.builder();

			ObjectOpenHashSet<String> allTags = new ObjectOpenHashSet<>();

			for (var entry : workingTagToKeys.object2ObjectEntrySet()) {
				if (!entry.getValue().isEmpty()) {
					tagToKeysBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
				}
				allTags.add(entry.getKey());
			}

			for (var entry : workingKeyToTags.object2ObjectEntrySet()) {
				keyToTagsBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
			}

			return new ExpandedMaps<>(tagToKeysBuilder.build(), keyToTagsBuilder.build(), ImmutableSet.copyOf(allTags));
		}

		@Desugar
		private record ExpandedMaps<T>(ImmutableMap<String, ImmutableSet<T>> tagToKeys,
				ImmutableMap<T, ImmutableSet<String>> keyToTags, ImmutableSet<String> allTags) {
		}
	}
}

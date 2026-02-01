package com.gardenevery.vintagetag;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

import com.github.bsideup.jabel.Desugar;
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

	private Tag(ImmutableMap<String, ImmutableSet<T>> tagToKeys, ImmutableMap<T, ImmutableSet<String>> keyToTags,
			ImmutableSet<String> tags) {
		this.tagToKeys = tagToKeys;
		this.keyToTags = keyToTags;
		this.tags = tags;
		this.associationCount = calculateAssociationCount();
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

		for (var tagName : tagNames) {
			if (tags.contains(tagName)) {
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
		return associationCount;
	}

	private int calculateAssociationCount() {
		int total = 0;
		for (Set<T> keys : tagToKeys.values()) {
			total += keys.size();
		}
		return total;
	}

	private enum ProcessingState {
		UNPROCESSED, PROCESSING, PROCESSED
	}

	static final class MutableTagContainer<T> {
		private final Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<T>> tagToKeys;
		private final Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<String>> tagInclusions;
		private final Object2ReferenceOpenHashMap<T, ObjectOpenHashSet<String>> keyToTags;

		public MutableTagContainer() {
			this.tagToKeys = new Object2ReferenceOpenHashMap<>();
			this.tagInclusions = new Object2ReferenceOpenHashMap<>();
			this.keyToTags = new Object2ReferenceOpenHashMap<>();
		}

		public void register(@Nonnull Set<T> keys, @Nonnull String tagName, @Nonnull Set<String> tagInclude) {
			Objects.requireNonNull(keys, "keys must not be null");
			Objects.requireNonNull(tagName, "tagName must not be null");
			Objects.requireNonNull(tagInclude, "tagInclude must not be null");

			if (keys.isEmpty() && tagInclude.isEmpty()) {
				return;
			}

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
			Objects.requireNonNull(keys, "keys must not be null");
			Objects.requireNonNull(tagName, "tagName must not be null");
			Objects.requireNonNull(tagInclude, "tagInclude must not be null");

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
			ExpandedMaps<T> expanded = expandMapsWithoutRecursion();
			return new Tag<>(expanded.tagToKeys, expanded.keyToTags, expanded.allTags);
		}

		public void clear() {
			tagToKeys.clear();
			tagInclusions.clear();
			keyToTags.clear();
		}

		private ExpandedMaps<T> expandMapsWithoutRecursion() {
			Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<T>> expandedTagToKeys = new Object2ReferenceOpenHashMap<>();
			Object2ReferenceOpenHashMap<T, ObjectOpenHashSet<String>> expandedKeyToTags = new Object2ReferenceOpenHashMap<>();

			for (var entry : tagToKeys.object2ReferenceEntrySet()) {
				var tag = entry.getKey();
				var keys = entry.getValue();
				expandedTagToKeys.put(tag, new ObjectOpenHashSet<>(keys));

				for (T key : keys) {
					expandedKeyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(tag);
				}
			}

			processTagInclusionsIteratively(expandedTagToKeys, expandedKeyToTags);

			ObjectOpenHashSet<String> allTags = new ObjectOpenHashSet<>();
			allTags.addAll(tagToKeys.keySet());
			allTags.addAll(tagInclusions.keySet());

			ImmutableMap.Builder<String, ImmutableSet<T>> tagToKeysBuilder = ImmutableMap.builder();
			for (var entry : expandedTagToKeys.object2ReferenceEntrySet()) {
				if (!entry.getValue().isEmpty()) {
					tagToKeysBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
				}
			}

			ImmutableMap.Builder<T, ImmutableSet<String>> keyToTagsBuilder = ImmutableMap.builder();
			for (var entry : expandedKeyToTags.object2ReferenceEntrySet()) {
				keyToTagsBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
			}

			return new ExpandedMaps<>(tagToKeysBuilder.build(), keyToTagsBuilder.build(), ImmutableSet.copyOf(allTags));
		}

		private void processTagInclusionsIteratively(
				Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<T>> expandedTagToKeys,
				Object2ReferenceOpenHashMap<T, ObjectOpenHashSet<String>> expandedKeyToTags) {

			Map<String, ProcessingState> state = new HashMap<>();

			for (var tag : tagToKeys.keySet()) {
				state.put(tag, ProcessingState.UNPROCESSED);
			}
			for (var tag : tagInclusions.keySet()) {
				state.putIfAbsent(tag, ProcessingState.UNPROCESSED);
			}

			for (var tag : state.keySet()) {
				if (state.get(tag) == ProcessingState.UNPROCESSED) {
					processTagInclusionIteratively(tag, state, expandedTagToKeys, expandedKeyToTags);
				}
			}
		}

		private void processTagInclusionIteratively(String startTag, Map<String, ProcessingState> state,
				Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<T>> expandedTagToKeys,
				Object2ReferenceOpenHashMap<T, ObjectOpenHashSet<String>> expandedKeyToTags) {

			Deque<ProcessingFrame> stack = new ArrayDeque<>();
			stack.push(new ProcessingFrame(startTag, null));

			while (!stack.isEmpty()) {
				var frame = stack.peek();
				var currentTag = frame.tag();

				var currentState = state.getOrDefault(currentTag, ProcessingState.UNPROCESSED);

				if (currentState == ProcessingState.PROCESSED) {
					stack.pop();
					if (frame.parentTag() != null) {
						inheritKeysFromTag(frame.parentTag(), currentTag, expandedTagToKeys, expandedKeyToTags);
					}
					continue;
				}

				if (currentState == ProcessingState.PROCESSING) {
					state.put(currentTag, ProcessingState.PROCESSED);
					stack.pop();
					if (frame.parentTag() != null) {
						inheritKeysFromTag(frame.parentTag(), currentTag, expandedTagToKeys, expandedKeyToTags);
					}
					continue;
				}

				state.put(currentTag, ProcessingState.PROCESSING);

				var dependencies = tagInclusions.get(currentTag);
				if (dependencies != null && !dependencies.isEmpty()) {
					for (var dependency : dependencies) {
						if (dependency.equals(currentTag)) {
							continue;
						}

						state.putIfAbsent(dependency, ProcessingState.UNPROCESSED);

						if (state.get(dependency) == ProcessingState.PROCESSING) {
							continue;
						}

						stack.push(new ProcessingFrame(dependency, currentTag));
					}
				} else {
					state.put(currentTag, ProcessingState.PROCESSED);
					stack.pop();

					if (frame.parentTag() != null) {
						inheritKeysFromTag(frame.parentTag(), currentTag, expandedTagToKeys, expandedKeyToTags);
					}
				}
			}
		}

		private void inheritKeysFromTag(String targetTag, String sourceTag,
				Object2ReferenceOpenHashMap<String, ObjectOpenHashSet<T>> expandedTagToKeys,
				Object2ReferenceOpenHashMap<T, ObjectOpenHashSet<String>> expandedKeyToTags) {

			var sourceKeys = expandedTagToKeys.get(sourceTag);
			if (sourceKeys == null || sourceKeys.isEmpty()) {
				return;
			}

			var targetKeys = expandedTagToKeys.computeIfAbsent(targetTag, k -> new ObjectOpenHashSet<>());

			targetKeys.addAll(sourceKeys);

			for (T key : sourceKeys) {
				expandedKeyToTags.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(targetTag);
			}
		}

		@Desugar
		private record ProcessingFrame(String tag, String parentTag) {
		}

		@Desugar
		private record ExpandedMaps<T>(ImmutableMap<String, ImmutableSet<T>> tagToKeys,
				ImmutableMap<T, ImmutableSet<String>> keyToTags, ImmutableSet<String> allTags) {
		}
	}
}

package com.polytech.knowledgebase.structure.search;

import com.polytech.knowledgebase.structure.domain.ConditionKind;
import com.polytech.knowledgebase.structure.domain.StatementCategory;
import com.polytech.knowledgebase.structure.domain.StatementStatus;
import com.polytech.knowledgebase.structure.domain.TopicType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight search candidate carrying only the fields used for filtering and ranking.
 * Statement text, condition text and evidence fragments stay in the database, so a broad
 * topic no longer materialises whole object graphs in memory.
 *
 * @param minValues condition kind to inclusive lower bound, null when the kind is categorical
 * @param maxValues condition kind to inclusive upper bound, null when the kind is categorical
 */
public record SearchCandidate(String id, String topicId, TopicType topicType, StatementStatus status,
                              StatementCategory category, Map<ConditionKind, Double> minValues,
                              Map<ConditionKind, Double> maxValues, Set<String> referenceIds) {

    public SearchCandidate {
        minValues = Collections.unmodifiableMap(minValues);
        maxValues = Collections.unmodifiableMap(maxValues);
        referenceIds = Set.copyOf(referenceIds);
    }

    /** Groups the flat projection rows into one candidate per statement, preserving order. */
    public static List<SearchCandidate> collect(List<Object[]> rows) {
        Map<String, Builder> builders = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Builder builder = builders.computeIfAbsent((String) row[0], id -> new Builder(row));
            builder.addCondition((ConditionKind) row[5], (Double) row[6], (Double) row[7]);
            builder.addReference((String) row[8]);
        }

        return builders.values().stream().map(Builder::build).toList();
    }

    public Set<ConditionKind> kinds() {
        return minValues.keySet();
    }

    public boolean isTopic(String otherTopicId) {
        return topicId.equals(otherTopicId);
    }

    public boolean references(String otherTopicId) {
        return referenceIds.contains(otherTopicId);
    }

    private static final class Builder {
        private final String id;
        private final String topicId;
        private final TopicType topicType;
        private final StatementStatus status;
        private final StatementCategory category;
        private final Map<ConditionKind, Double> minValues = new EnumMap<>(ConditionKind.class);
        private final Map<ConditionKind, Double> maxValues = new EnumMap<>(ConditionKind.class);
        private final Set<String> referenceIds = new LinkedHashSet<>();

        private Builder(Object[] row) {
            this.id = (String) row[0];
            this.topicId = (String) row[1];
            this.topicType = (TopicType) row[2];
            this.status = (StatementStatus) row[3];
            this.category = (StatementCategory) row[4];
        }

        private void addCondition(ConditionKind kind, Double minValue, Double maxValue) {
            if (kind == null) {
                return;
            }

            minValues.put(kind, minValue);
            maxValues.put(kind, maxValue);
        }

        private void addReference(String referenceId) {
            if (referenceId != null) {
                referenceIds.add(referenceId);
            }
        }

        private SearchCandidate build() {
            return new SearchCandidate(id, topicId, topicType, status, category,
                    minValues, maxValues, referenceIds);
        }
    }
}
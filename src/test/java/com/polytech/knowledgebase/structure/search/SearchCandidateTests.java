package com.polytech.knowledgebase.structure.search;

import com.polytech.knowledgebase.structure.domain.ConditionKind;
import com.polytech.knowledgebase.structure.domain.StatementCategory;
import com.polytech.knowledgebase.structure.domain.StatementStatus;
import com.polytech.knowledgebase.structure.domain.TopicType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCandidateTests {

    @Test
    void groupsFlatRowsIntoOneCandidatePerStatement() {
        List<SearchCandidate> candidates = SearchCandidate.collect(List.of(
                row("a", "topic-1", StatementStatus.VERIFIED, ConditionKind.CALM_WATER, null, null, "topic-2"),
                row("a", "topic-1", StatementStatus.VERIFIED, ConditionKind.WIND_SPEED, 0.0, 5.0, "topic-3"),
                row("a", "topic-1", StatementStatus.VERIFIED, ConditionKind.CALM_WATER, null, null, "topic-2"),
                row("b", "topic-2", StatementStatus.DEMO, null, null, null, null)));

        assertThat(candidates).extracting(SearchCandidate::id).containsExactly("a", "b");
        SearchCandidate first = candidates.getFirst();
        assertThat(first.kinds()).containsExactlyInAnyOrder(ConditionKind.CALM_WATER, ConditionKind.WIND_SPEED);
        assertThat(first.referenceIds()).containsExactlyInAnyOrder("topic-2", "topic-3");
        assertThat(first.isTopic("topic-1")).isTrue();
        assertThat(first.references("topic-2")).isTrue();
        assertThat(first.references("topic-9")).isFalse();
        assertThat(first.minValues()).containsEntry(ConditionKind.WIND_SPEED, 0.0);
        assertThat(first.maxValues()).containsEntry(ConditionKind.WIND_SPEED, 5.0);
    }

    @Test
    void keepsStatementsWithoutConditionsAndReferences() {
        List<SearchCandidate> candidates = SearchCandidate.collect(
                List.<Object[]>of(row("a", "topic-1", StatementStatus.VERIFIED, null, null, null, null)));

        SearchCandidate candidate = candidates.getFirst();
        assertThat(candidate.kinds()).isEmpty();
        assertThat(candidate.referenceIds()).isEmpty();
    }

    @Test
    void keepsNullBoundsForCategoricalConditions() {
        List<SearchCandidate> candidates = SearchCandidate.collect(
                List.<Object[]>of(row("a", "topic-1", StatementStatus.VERIFIED, ConditionKind.BEGINNER, null, null, null)));

        SearchCandidate candidate = candidates.getFirst();
        assertThat(candidate.kinds()).containsExactly(ConditionKind.BEGINNER);
        assertThat(candidate.minValues().get(ConditionKind.BEGINNER)).isNull();
        assertThat(candidate.maxValues().get(ConditionKind.BEGINNER)).isNull();
    }

    private Object[] row(String id, String topicId, StatementStatus status, ConditionKind kind,
                         Double minValue, Double maxValue, String referenceId) {
        return new Object[]{id, topicId, TopicType.SPORT, status, StatementCategory.CONDITIONS,
                kind, minValue, maxValue, referenceId};
    }
}
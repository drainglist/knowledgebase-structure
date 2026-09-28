package com.polytech.knowledgebase.structure.search;

import com.polytech.knowledgebase.structure.domain.*;
import com.polytech.knowledgebase.structure.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.polytech.knowledgebase.structure.search.SearchResult.*;

@Service
public class KnowledgeSearchService {
    private static final int RESULT_LIMIT = 5;
    private final TopicRepository topics;
    private final StatementRepository statements;
    private final SearchVocabulary vocabulary;

    public KnowledgeSearchService(TopicRepository topics, StatementRepository statements, SearchVocabulary vocabulary) {
        this.topics = topics;
        this.statements = statements;
        this.vocabulary = vocabulary;
    }

    @Transactional(readOnly = true)
    public SearchResult search(String question, boolean includeDemo) {
        String text = TextNormalizer.normalize(question);
        StatementCategory category = vocabulary.category(text).orElse(null);
        if (text.isEmpty()) return new SearchResult(List.of(), category, List.of(), "Введите непустой вопрос.");
        List<Topic> matches = topics.findAllByOrderByIdAsc().stream()
                .filter(t -> TextNormalizer.containsPhrase(text, t.getName())
                    || t.getSynonyms().stream().anyMatch(s -> TextNormalizer.containsPhrase(text, s.getName())))
                .toList();
        List<Topic> sports = matches.stream().filter(t -> t.getType() == TopicType.SPORT).toList();
        List<Topic> selected = sports.isEmpty() ? matches : sports;
        List<TopicView> views = selected.stream().map(t -> new TopicView(t.getId(), t.getName())).toList();
        if (selected.isEmpty()) return new SearchResult(views, category, List.of(), "Тема не найдена. Попробуйте другое название.");
        if (selected.size() > 1) return new SearchResult(views, category, List.of(),
                "Обнаружены несколько тем. Уточните одну тему; сравнение пока не поддерживается.");

        Set<ConditionKind> hints = vocabulary.conditions(text);
        if (hints.stream().map(ConditionKind::getDimension).distinct().count() != hints.size())
            return new SearchResult(views, category, List.of(), "Обнаружены разные условия одной группы. Уточните условия.");
        List<StatementStatus> allowed = includeDemo
                ? List.of(StatementStatus.VERIFIED, StatementStatus.DEMO) : List.of(StatementStatus.VERIFIED);
        StatementCategory effectiveCategory = category == null ? StatementCategory.GENERAL : category;
        List<StatementView> result = statements.findByTopicIdAndStatusInOrderByIdAsc(selected.getFirst().getId(), allowed)
                .stream()
                .filter(s -> allowed.contains(s.getStatus()))
                .filter(s -> s.getCategory() == effectiveCategory)
                .filter(s -> compatible(s, hints))
                .sorted(Comparator.<Statement>comparingLong(s -> matchingConditions(s, hints)).reversed()
                        .thenComparing(Statement::getId))
                .limit(RESULT_LIMIT)
                .map(this::snapshot).toList();
        return new SearchResult(views, category, result,
                result.isEmpty() ? "Подходящих утверждений с допустимым статусом и условиями нет." :
                    category == null ? "Категория не определена: показаны общие сведения (не более 5)." : "");
    }

    // Unmentioned conditions are retained and printed; explicit conflicts are excluded.
    private boolean compatible(Statement statement, Set<ConditionKind> hints) {
        return statement.getConditions().stream().noneMatch(c -> hints.stream()
                .anyMatch(h -> h.getDimension().equals(c.getKind().getDimension()) && h != c.getKind()));
    }
    private long matchingConditions(Statement statement, Set<ConditionKind> hints) {
        return statement.getConditions().stream().filter(c -> hints.contains(c.getKind())).count();
    }
    private StatementView snapshot(Statement s) {
        return new StatementView(s.getId(), s.getText(), s.getCategory(), s.getStatus(),
                s.getConditions().stream().map(StatementCondition::getText).toList(),
                s.getEvidence().stream().map(e -> new SourceView(e.getSource().getTitle(),
                        e.getSource().getLocation(), e.getSource().isDemonstration(),
                        e.getFragment(), e.getLocator())).toList());
    }
}

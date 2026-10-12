package com.polytech.knowledgebase.structure.search;

import com.polytech.knowledgebase.structure.domain.ConditionKind;
import com.polytech.knowledgebase.structure.domain.Statement;
import com.polytech.knowledgebase.structure.domain.StatementCategory;
import com.polytech.knowledgebase.structure.domain.StatementCondition;
import com.polytech.knowledgebase.structure.domain.StatementStatus;
import com.polytech.knowledgebase.structure.domain.Topic;
import com.polytech.knowledgebase.structure.domain.TopicType;
import com.polytech.knowledgebase.structure.repository.StatementRepository;
import com.polytech.knowledgebase.structure.repository.TopicRelationRepository;
import com.polytech.knowledgebase.structure.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.polytech.knowledgebase.structure.search.SearchResult.ConditionView;
import static com.polytech.knowledgebase.structure.search.SearchResult.ReviewView;
import static com.polytech.knowledgebase.structure.search.SearchResult.SourceView;
import static com.polytech.knowledgebase.structure.search.SearchResult.StatementView;
import static com.polytech.knowledgebase.structure.search.SearchResult.TopicView;

@Service
public class KnowledgeSearchService {
    private static final int RESULT_LIMIT = 5;
    private final TopicRepository topics;
    private final StatementRepository statements;
    private final TopicRelationRepository relations;
    private final SearchVocabulary vocabulary;

    public KnowledgeSearchService(TopicRepository topics, StatementRepository statements,
                                  TopicRelationRepository relations, SearchVocabulary vocabulary) {
        this.topics = topics;
        this.statements = statements;
        this.relations = relations;
        this.vocabulary = vocabulary;
    }

    @Transactional(readOnly = true)
    public SearchResult search(String question, boolean includeDemo) {
        SearchRequest request = new SearchRequest(question, null, List.of(), Set.of(), Map.of(),
                includeDemo, 0, RESULT_LIMIT);
        return search(request, true);
    }

    @Transactional(readOnly = true)
    public SearchResult search(SearchRequest request) {
        return search(request, false);
    }

    private SearchResult search(SearchRequest request, boolean generalFallback) {
        String text = TextNormalizer.normalize(request.question());
        List<StatementCategory> categories = request.categories().isEmpty()
                ? vocabulary.categories(text) : request.categories();
        StatementCategory category = categories.isEmpty() ? null : categories.getFirst();

        if (generalFallback && categories.size() > 1) {
            categories = List.of(categories.getFirst());
        }

        if (text.isEmpty() && request.topicId() == null) {
            return empty(request, List.of(), category, "Введите непустой вопрос или выберите тему.", List.of());
        }

        List<Topic> catalog = topics.findAllByOrderByIdAsc();
        List<Topic> matches = catalog.stream().filter(topic -> mentions(text, topic)).toList();
        List<Topic> selected = selectTopics(request, catalog, matches);
        List<TopicView> views = selected.stream().map(this::topicView).toList();

        if (selected.isEmpty()) {
            return empty(request, views, category, "Тема не найдена. Попробуйте другое название.",
                    suggestions(text, catalog));
        }

        if (selected.size() > 1) {
            return empty(request, views, category,
                    "Обнаружены несколько тем. Уточните одну тему; сравнение пока не поддерживается.", List.of());
        }

        QueryConditions conditions = queryConditions(request, text);
        if (conditions.hints().stream().map(ConditionKind::getDimension).distinct().count()
                != conditions.hints().size()) {
            return empty(request, views, category,
                    "Обнаружены разные условия одной группы. Уточните условия.", List.of());
        }

        if (categories.isEmpty()) {
            categories = generalFallback ? List.of(StatementCategory.GENERAL) : List.of(StatementCategory.values());
        }

        Topic topic = selected.getFirst();
        boolean useRelations = topic.getType() != TopicType.SPORT
                || categories.contains(StatementCategory.EQUIPMENT) || categories.contains(StatementCategory.CONDITIONS);
        Set<String> primaryTopicIds = useRelations ? linkedTopicIds(topic) : Set.of(topic.getId());
        List<StatementStatus> allowed = request.includeDemo()
                ? List.of(StatementStatus.VERIFIED, StatementStatus.DEMO) : List.of(StatementStatus.VERIFIED);

        List<SearchCandidate> candidates = SearchCandidate.collect(
                        statements.findSearchProjection(primaryTopicIds, Set.of(topic.getId()), allowed, categories))
                .stream()
                .filter(candidate -> topic.getType() != TopicType.SPORT
                        || candidate.topicType() != TopicType.SPORT
                        || candidate.isTopic(topic.getId()))
                .filter(candidate -> compatible(candidate, conditions))
                .sorted(ranking(topic, matches, conditions))
                .toList();

        long offset = (long) request.page() * request.size();
        List<StatementView> result = page(topic, conditions,
                candidates.stream().skip(offset).limit(request.size()).toList());

        String message = candidates.isEmpty() ? "Подходящих утверждений с допустимым статусом и условиями нет."
                : result.isEmpty() ? "На этой странице нет результатов."
                : category == null && generalFallback ? "Категория не определена: показаны общие сведения (не более 5)."
                : "";

        return new SearchResult(views, category, result, message, candidates.size(), request.page(), request.size(),
                conditionLabels(conditions), List.of());
    }

    private List<Topic> selectTopics(SearchRequest request, List<Topic> catalog, List<Topic> matches) {
        if (request.topicId() != null) {
            return catalog.stream().filter(topic -> topic.getId().equals(request.topicId())).toList();
        }

        List<Topic> sports = matches.stream().filter(topic -> topic.getType() == TopicType.SPORT).toList();
        return sports.isEmpty() ? matches : sports;
    }

    private boolean mentions(String text, Topic topic) {
        return TextNormalizer.containsUnnegatedPhrase(text, topic.getName())
                || topic.getSynonyms().stream()
                        .anyMatch(synonym -> TextNormalizer.containsUnnegatedPhrase(text, synonym.getName()));
    }

    private Set<String> linkedTopicIds(Topic topic) {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(topic.getId());

        relations.findAdjacent(topic.getId()).forEach(relation -> {
            Topic neighbour = relation.getFromTopic().getId().equals(topic.getId())
                    ? relation.getToTopic() : relation.getFromTopic();
            if (topic.getType() != TopicType.SPORT || neighbour.getType() != TopicType.SPORT) {
                ids.add(neighbour.getId());
            }
        });

        return ids;
    }

    private QueryConditions queryConditions(SearchRequest request, String text) {
        Set<ConditionKind> hints = vocabulary.conditions(text);
        Set<ConditionKind> excluded = vocabulary.negatedConditions(text);
        Set<String> explicitDimensions = request.conditions().stream()
                .map(ConditionKind::getDimension).collect(Collectors.toSet());
        hints.removeIf(kind -> explicitDimensions.contains(kind.getDimension()));
        excluded.removeIf(kind -> explicitDimensions.contains(kind.getDimension()));
        hints.addAll(request.conditions());

        Map<ConditionKind, Double> numeric = new EnumMap<>(ConditionKind.class);
        numeric.putAll(vocabulary.numericConditions(request.question()));
        numeric.putAll(request.numericConditions());
        return new QueryConditions(hints, excluded, numeric);
    }

    // Unknown restrictions stay visible, while explicit conflicts are excluded.
    private boolean compatible(SearchCandidate candidate, QueryConditions query) {
        for (ConditionKind kind : candidate.kinds()) {
            if (query.excluded().contains(kind)) {
                return false;
            }

            if (!kind.isNumeric() && query.hints().stream()
                    .anyMatch(hint -> hint.getDimension().equals(kind.getDimension()) && hint != kind)) {
                return false;
            }

            Double value = query.numeric().get(kind);
            if (value != null && !inRange(value, candidate, kind)) {
                return false;
            }
        }

        return true;
    }

    private boolean inRange(double value, SearchCandidate candidate, ConditionKind kind) {
        Double minValue = candidate.minValues().get(kind);
        Double maxValue = candidate.maxValues().get(kind);
        return (minValue == null || value >= minValue) && (maxValue == null || value <= maxValue);
    }

    private long matchingConditions(SearchCandidate candidate, QueryConditions query) {
        return candidate.kinds().stream().filter(kind -> query.hints().contains(kind)
                || query.numeric().containsKey(kind)).count();
    }

    // Full objects are read only for the rows that reach the current page.
    private List<StatementView> page(Topic topic, QueryConditions conditions, List<SearchCandidate> page) {
        if (page.isEmpty()) {
            return List.of();
        }

        Map<String, Statement> details = statements
                .findPageWithDetails(page.stream().map(SearchCandidate::id).toList())
                .stream()
                .collect(Collectors.toMap(Statement::getId, Function.identity()));

        List<StatementView> views = new ArrayList<>(page.size());
        for (SearchCandidate candidate : page) {
            Statement statement = details.get(candidate.id());
            if (statement != null) {
                views.add(snapshot(statement, candidate, topic, conditions));
            }
        }

        return List.copyOf(views);
    }

    private Comparator<SearchCandidate> ranking(Topic topic, List<Topic> matches, QueryConditions conditions) {
        Set<String> secondaryTopics = matches.stream().filter(match -> !match.getId().equals(topic.getId()))
                .map(Topic::getId).collect(Collectors.toSet());

        return Comparator.<SearchCandidate>comparingInt(candidate -> topicRank(candidate, topic)).reversed()
                .thenComparing(candidate -> candidate.status() != StatementStatus.VERIFIED)
                .thenComparing(Comparator.<SearchCandidate>comparingLong(candidate -> matchingConditions(candidate, conditions))
                        .reversed())
                .thenComparing(Comparator.<SearchCandidate>comparingLong(candidate -> candidate.referenceIds().stream()
                        .filter(secondaryTopics::contains).count()).reversed())
                .thenComparing(SearchCandidate::id);
    }

    private int topicRank(SearchCandidate candidate, Topic topic) {
        if (candidate.isTopic(topic.getId())) {
            return 3;
        }

        return candidate.references(topic.getId()) ? 2 : 1;
    }

    private String matchReason(SearchCandidate candidate, Topic topic) {
        return switch (topicRank(candidate, topic)) {
            case 3 -> "Основная тема совпадает: " + topic.getName();
            case 2 -> "Утверждение ссылается на тему: " + topic.getName();
            default -> "Основная тема связана с «" + topic.getName() + "»";
        };
    }

    private String applicability(SearchCandidate candidate, QueryConditions conditions) {
        if (candidate.kinds().isEmpty()) {
            return "UNSPECIFIED";
        }

        return matchingConditions(candidate, conditions) == candidate.kinds().size()
                ? "MATCHED" : "CONDITIONAL";
    }

    private StatementView snapshot(Statement statement, SearchCandidate candidate, Topic topic,
                                   QueryConditions conditions) {
        ReviewView review = statement.getReviewedAt() == null ? null
                : new ReviewView(statement.getReviewedBy(), statement.getReviewedAt(), statement.getReviewNote());

        return new StatementView(statement.getId(), statement.getText(), statement.getCategory(), statement.getStatus(),
                statement.getConditions().stream().map(StatementCondition::getText).toList(),
                statement.getEvidence().stream().map(evidence -> new SourceView(evidence.getSource().getTitle(),
                        evidence.getSource().getLocation(), evidence.getSource().isDemonstration(),
                        evidence.getFragment(), evidence.getLocator())).toList(),
                topicView(statement.getTopic()), statement.getReferencedTopics().stream().map(this::topicView).toList(),
                statement.getConditions().stream().map(condition -> new ConditionView(condition.getKind(),
                        condition.getText(), condition.getMinValue(), condition.getMaxValue(), condition.getUnit())).toList(),
                matchReason(candidate, topic), applicability(candidate, conditions), review);
    }

    private TopicView topicView(Topic topic) {
        return new TopicView(topic.getId(), topic.getName());
    }

    private SearchResult empty(SearchRequest request, List<TopicView> topics, StatementCategory category,
                               String message, List<String> suggestions) {
        return new SearchResult(topics, category, List.of(), message, 0, request.page(), request.size(),
                List.of(), suggestions);
    }

    private List<String> conditionLabels(QueryConditions query) {
        List<String> labels = new ArrayList<>();
        query.hints().stream().sorted().map(ConditionKind::getLabel).forEach(labels::add);
        query.excluded().stream().sorted().map(kind -> "Исключено: " + kind.getLabel()).forEach(labels::add);
        query.numeric().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().getLabel() + ": " + entry.getValue() + " " + entry.getKey().getUnit())
                .forEach(labels::add);
        return labels;
    }

    private List<String> suggestions(String text, List<Topic> catalog) {
        Set<String> suggestions = new LinkedHashSet<>();
        for (String token : text.split(" ")) {
            if (token.length() < 5) {
                continue;
            }

            List<Topic> close = catalog.stream().filter(topic -> closeName(token, topic.getName())
                    || topic.getSynonyms().stream().anyMatch(synonym -> closeName(token, synonym.getName()))).toList();
            if (close.size() == 1) {
                suggestions.add(close.getFirst().getName());
            }
        }

        return suggestions.stream().limit(3).toList();
    }

    private boolean closeName(String token, String name) {
        String candidate = TextNormalizer.normalize(name);
        if (candidate.contains(" ") || candidate.length() < 5 || Math.abs(candidate.length() - token.length()) > 1) {
            return false;
        }

        int[] previous = new int[candidate.length() + 1];
        for (int index = 0; index < previous.length; index++) {
            previous[index] = index;
        }

        for (int row = 1; row <= token.length(); row++) {
            int[] current = new int[candidate.length() + 1];
            current[0] = row;
            for (int column = 1; column <= candidate.length(); column++) {
                int substitution = previous[column - 1]
                        + (token.charAt(row - 1) == candidate.charAt(column - 1) ? 0 : 1);
                current[column] = Math.min(substitution, Math.min(current[column - 1] + 1, previous[column] + 1));
            }
            previous = current;
        }

        return previous[candidate.length()] == 1;
    }

    private record QueryConditions(Set<ConditionKind> hints, Set<ConditionKind> excluded,
                                   Map<ConditionKind, Double> numeric) {}
}

package com.polytech.knowledgebase.structure.search;

import com.polytech.knowledgebase.structure.domain.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class SearchVocabulary {
    private final Map<StatementCategory, List<String>> categories = new LinkedHashMap<>();
    private final Map<ConditionKind, List<String>> conditions = new LinkedHashMap<>();

    public SearchVocabulary() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                new ClassPathResource("search-dictionary.properties").getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        for (String key : properties.getProperty("category.priority").split(",")) {
            StatementCategory category = StatementCategory.valueOf(key.strip());
            categories.put(category, phrases(properties, "category." + category));
        }
        for (ConditionKind kind : ConditionKind.values())
            conditions.put(kind, phrases(properties, "condition." + kind));
    }
    private List<String> phrases(Properties properties, String key) {
        return Arrays.stream(properties.getProperty(key, "").split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
    public Optional<StatementCategory> category(String text) {
        return categories.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(p -> TextNormalizer.containsPhrase(text, p)))
                .map(Map.Entry::getKey).findFirst();
    }
    public Set<ConditionKind> conditions(String text) {
        Set<ConditionKind> result = EnumSet.noneOf(ConditionKind.class);
        conditions.forEach((kind, phrases) -> {
            if (phrases.stream().anyMatch(p -> TextNormalizer.containsPhrase(text, p))) result.add(kind);
        });
        return result;
    }
}

package com.polytech.knowledgebase.structure.search;

import com.polytech.knowledgebase.structure.domain.*;
import java.util.List;

// Detached snapshots: console output never traverses lazy JPA associations.
public record SearchResult(List<TopicView> topics, StatementCategory category,
                           List<StatementView> statements, String message) {
    public SearchResult { topics = List.copyOf(topics); statements = List.copyOf(statements); }
    public record TopicView(String id, String name) {}
    public record SourceView(String title, String location, boolean demonstration, String fragment, String locator) {}
    public record StatementView(String id, String text, StatementCategory category, StatementStatus status,
                                List<String> conditions, List<SourceView> sources) {
        public StatementView { conditions = List.copyOf(conditions); sources = List.copyOf(sources); }
    }
}

package com.polytech.knowledgebase.structure.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.util.*;

@Entity
@Table(name = "topic_synonyms", uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "name"}))
@Check(constraints = "length(trim(id)) > 0 and length(trim(name)) > 0")
public class TopicSynonym extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_id", nullable = false) private Topic topic;
    @Column(nullable = false, length = 200) private String name;
    protected TopicSynonym() {}
    public TopicSynonym(String id, Topic topic, String name) {
        super(id); this.topic = Objects.requireNonNull(topic); this.name = required(name, "name", 200);
    }
    public Topic getTopic() { return topic; } public String getName() { return name; }
}

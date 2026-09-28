package com.polytech.knowledgebase.structure.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.util.*;

@Entity
@Table(name = "topics")
@Check(constraints = "length(trim(id)) > 0 and length(trim(name)) > 0")
public class Topic extends BaseEntity {
    @Column(nullable = false, length = 200, unique = true) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TopicType type;
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("id") private Set<TopicSynonym> synonyms = new LinkedHashSet<>();
    protected Topic() {}
    public Topic(String id, String name, TopicType type) {
        super(id); this.name = required(name, "name", 200); this.type = Objects.requireNonNull(type);
    }
    public void addSynonym(String id, String name) { synonyms.add(new TopicSynonym(id, this, name)); }
    public Set<TopicSynonym> getSynonyms() { return Collections.unmodifiableSet(synonyms); }
    public String getName() { return name; } public TopicType getType() { return type; }
}

package com.polytech.knowledgebase.structure.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.util.*;

@Entity
@Table(name = "statements", indexes = @Index(name = "idx_statement_search", columnList = "topic_id,status,category"))
@Check(constraints = "length(trim(id)) > 0 and length(trim(text)) > 0")
public class Statement extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_id", nullable = false) private Topic topic;
    @Column(nullable = false, length = 4000) private String text;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private StatementCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private StatementStatus status;
    @ManyToMany @JoinTable(name = "statement_references", joinColumns = @JoinColumn(name = "statement_id"), inverseJoinColumns = @JoinColumn(name = "topic_id"), uniqueConstraints = @UniqueConstraint(columnNames = {"statement_id", "topic_id"}))
private Set<Topic> referencedTopics = new LinkedHashSet<>();
    @OneToMany(mappedBy = "statement", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("id") private Set<StatementCondition> conditions = new LinkedHashSet<>();
    @OneToMany(mappedBy = "statement", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("id") private Set<Evidence> evidence = new LinkedHashSet<>();
    protected Statement() {}
    public Statement(String id, Topic topic, String text, StatementCategory category, StatementStatus status) {
        super(id); this.topic = Objects.requireNonNull(topic); this.text = required(text, "text", 4000);
        this.category = Objects.requireNonNull(category); this.status = Objects.requireNonNull(status);
    }
    public void addReference(Topic topic) { referencedTopics.add(Objects.requireNonNull(topic)); }
    public void addCondition(String id, ConditionKind kind, String text) { conditions.add(new StatementCondition(id, this, kind, text)); }
    public void addEvidence(String id, Source source, String fragment, String locator) { evidence.add(new Evidence(id, this, source, fragment, locator)); }
    public Set<Topic> getReferencedTopics() { return Collections.unmodifiableSet(referencedTopics); }
    public Set<StatementCondition> getConditions() { return Collections.unmodifiableSet(conditions); }
    public Set<Evidence> getEvidence() { return Collections.unmodifiableSet(evidence); }
    public Topic getTopic() { return topic; } public String getText() { return text; } public StatementCategory getCategory() { return category; } public StatementStatus getStatus() { return status; }
}

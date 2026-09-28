package com.polytech.knowledgebase.structure.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.util.*;

@Entity
@Table(name = "statement_conditions", uniqueConstraints = @UniqueConstraint(columnNames = {"statement_id", "kind"}))
@Check(constraints = "length(trim(id)) > 0 and length(trim(text)) > 0")
public class StatementCondition extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "statement_id", nullable = false) private Statement statement;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ConditionKind kind;
    @Column(nullable = false, length = 500) private String text;
    protected StatementCondition() {}
    public StatementCondition(String id, Statement statement, ConditionKind kind, String text) {
        super(id); this.statement = Objects.requireNonNull(statement); this.kind = Objects.requireNonNull(kind); this.text = required(text, "text", 500);
    }
    public Statement getStatement() { return statement; } public ConditionKind getKind() { return kind; } public String getText() { return text; }
}

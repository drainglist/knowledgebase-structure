package com.polytech.knowledgebase.structure.domain;
import jakarta.persistence.*;

@MappedSuperclass
public abstract class BaseEntity {
    @Id @Column(length = 120, nullable = false, updatable = false)
    private String id;
    protected BaseEntity() {}
    protected BaseEntity(String id) { this.id = required(id, "id", 120); }
    public String getId() { return id; }
    protected static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength)
            throw new IllegalArgumentException(field + " must be non-blank and at most " + maxLength + " characters");
        return value.strip();
    }
}

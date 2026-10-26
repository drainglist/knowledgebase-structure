package com.polytech.knowledgebase.structure.document;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents", check =
        @CheckConstraint(name = "ck_document_title", constraint = "length(trim(title)) > 0"))
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 300)
    private String author;

    @Column(length = 2048)
    private String sourceUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Document() {
    }

    public Document(String title, String author, String sourceUrl) {
        this.title = requiredTitle(title);
        this.author = optional(author, "Автор", 300);
        this.sourceUrl = validSourceUrl(sourceUrl);
        this.createdAt = Instant.now();
    }

    private static String requiredTitle(String value) {
        String title = optional(value, "Название", 500);
        if (title == null) {
            throw new IllegalArgumentException("Укажите название статьи.");
        }

        return title;
    }

    private static String optional(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + ": допустимо не более " + maxLength + " символов.");
        }

        return normalized;
    }

    private static String validSourceUrl(String value) {
        String url = optional(value, "Ссылка на источник", 2048);
        if (url == null) {
            return null;
        }

        try {
            URI uri = URI.create(url);
            if (uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return url;
            }
        } catch (IllegalArgumentException ignored) {
            // Malformed and unsupported URLs share one validation message.
        }

        throw new IllegalArgumentException("Укажите полную HTTP- или HTTPS-ссылку на источник.");
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

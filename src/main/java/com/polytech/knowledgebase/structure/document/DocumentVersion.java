package com.polytech.knowledgebase.structure.document;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "document_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_document_version",
                columnNames = {"document_id", "version_number"}),
        check = @CheckConstraint(name = "ck_document_version",
                constraint = "version_number > 0 and length(trim(text)) > 0"))
public class DocumentVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, updatable = false)
    private Document document;

    @Column(name = "version_number", nullable = false, updatable = false)
    private int number;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String text;

    @Column(nullable = false, updatable = false, length = 64)
    private String contentHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DocumentVersion() {
    }

    public DocumentVersion(Document document, String text) {
        this.document = Objects.requireNonNull(document, "Укажите статью для версии.");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Укажите непустой текст статьи.");
        }

        this.number = 1;
        this.text = text;
        this.contentHash = sha256(text);
        this.createdAt = Instant.now();
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 недоступен в текущей Java.", exception);
        }
    }

    public UUID getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public int getNumber() {
        return number;
    }

    public String getText() {
        return text;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

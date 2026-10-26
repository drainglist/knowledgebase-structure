package com.polytech.knowledgebase.structure.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentDetails(UUID id, String title, String author, String sourceUrl,
                              Instant createdAt, Version version) {
    public record Version(UUID id, int number, String text, String contentHash, Instant createdAt) {
    }
}

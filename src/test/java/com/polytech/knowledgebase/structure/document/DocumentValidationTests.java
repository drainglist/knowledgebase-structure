package com.polytech.knowledgebase.structure.document;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentValidationTests {
    @Test
    void requiresATitleAndLimitsMetadataLength() {
        assertThatThrownBy(() -> new Document(" \n", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Document("a".repeat(501), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Document("Статья", "a".repeat(301), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsOnlyCompleteHttpSourceUrls() {
        assertThatThrownBy(() -> new Document("Статья", null, "/articles/sup"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Document("Статья", null, "file:///private/article.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Document("Статья", null, "https://bad host/article"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(new Document("Статья", null, " HTTPS://example.org/article ").getSourceUrl())
                .isEqualTo("HTTPS://example.org/article");
    }

    @Test
    void requiresTextAndCalculatesAKnownSha256Checksum() {
        Document document = new Document("Статья", null, null);
        assertThatThrownBy(() -> new DocumentVersion(document, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DocumentVersion(document, "\r\n\t"))
                .isInstanceOf(IllegalArgumentException.class);

        DocumentVersion version = new DocumentVersion(document, "abc");
        assertThat(version.getText()).isEqualTo("abc");
        assertThat(version.getNumber()).isEqualTo(1);
        assertThat(version.getContentHash())
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}

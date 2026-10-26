package com.polytech.knowledgebase.structure.document;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DocumentStorageTests {
    @Autowired DocumentService service;
    @Autowired DocumentRepository documents;
    @Autowired DocumentVersionRepository versions;
    @Autowired EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void clearDocuments() {
        versions.deleteAllInBatch();
        documents.deleteAllInBatch();
    }

    @Test
    void storesAndReloadsALongArticleWithoutChangingItsText() {
        String text = "  Введение\r\n\r\n"
                + "Снаряжение, условия и безопасность: ё, SUP, 🛶.\r\n".repeat(600)
                + "\n  Конец статьи.\n";

        DocumentDetails created = service.create("  Статья о водных видах спорта  ",
                "  Автор статьи  ", "https://example.org/articles/sup", text);
        DocumentDetails loaded = service.get(created.id());

        assertThat(loaded.id()).isEqualTo(created.id());
        assertThat(loaded.title()).isEqualTo("Статья о водных видах спорта");
        assertThat(loaded.author()).isEqualTo("Автор статьи");
        assertThat(loaded.sourceUrl()).isEqualTo("https://example.org/articles/sup");
        assertThat(loaded.createdAt()).isNotNull();
        assertThat(loaded.version().id()).isEqualTo(created.version().id());
        assertThat(loaded.version().number()).isEqualTo(1);
        assertThat(loaded.version().text()).isEqualTo(text).hasSizeGreaterThan(4000);
        assertThat(loaded.version().contentHash()).isEqualTo(created.version().contentHash()).hasSize(64);
        assertThat(loaded.version().createdAt()).isNotNull();
        assertThat(documents.count()).isEqualTo(1);
        assertThat(versions.count()).isEqualTo(1);
    }

    @Test
    void allowsAnUnknownAuthorAndAnAbsentSourceUrl() {
        DocumentDetails created = service.create("Статья без метаданных", " \t", null, "Полный текст.");

        DocumentDetails loaded = service.get(created.id());

        assertThat(loaded.author()).isNull();
        assertThat(loaded.sourceUrl()).isNull();
        assertThat(loaded.version().text()).isEqualTo("Полный текст.");
    }

    @Test
    void failedVersionCreationDoesNotLeaveAnOrphanDocument() {
        assertThatThrownBy(() -> service.create("Не сохранится", null, null, " \r\n\t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("текст");

        assertThat(documents.count()).isZero();
        assertThat(versions.count()).isZero();
    }

    @Test
    void databaseRejectsADuplicateFirstVersionWithoutReplacingTheOriginal() {
        DocumentDetails created = service.create("Первая версия", null, null, "Исходный текст.");
        Document document = documents.findById(created.id()).orElseThrow();

        assertThatThrownBy(() -> versions.saveAndFlush(new DocumentVersion(document, "Другой текст.")))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(versions.count()).isEqualTo(1);
        assertThat(service.get(created.id()).version().text()).isEqualTo("Исходный текст.");
    }

    @Test
    void reportsAMissingDocument() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Статья не найдена.");
    }

    @Test
    void activeApplicationContainsOnlyTheNewDocumentEntities() {
        assertThat(entityManagerFactory.getMetamodel().getEntities())
                .extracting(type -> type.getJavaType().getName())
                .containsExactlyInAnyOrder(Document.class.getName(), DocumentVersion.class.getName());
        assertThat(getClass().getResource("/static/index.html")).isNull();
        assertThat(getClass().getResource("/demo-statements.tsv")).isNull();
    }
}

package com.polytech.knowledgebase.structure.document;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocumentService {
    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;

    public DocumentService(DocumentRepository documents, DocumentVersionRepository versions) {
        this.documents = documents;
        this.versions = versions;
    }

    @Transactional
    public DocumentDetails create(String title, String author, String sourceUrl, String text) {
        Document document = documents.save(new Document(title, author, sourceUrl));
        DocumentVersion version = versions.save(new DocumentVersion(document, text));
        return details(document, version);
    }

    @Transactional(readOnly = true)
    public DocumentDetails get(UUID id) {
        Objects.requireNonNull(id, "Укажите идентификатор статьи.");
        Document document = documents.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Статья не найдена."));
        DocumentVersion version = versions.findByDocumentIdAndNumber(id, 1)
                .orElseThrow(() -> new IllegalStateException("У статьи отсутствует первая версия."));

        return details(document, version);
    }

    private static DocumentDetails details(Document document, DocumentVersion version) {
        return new DocumentDetails(document.getId(), document.getTitle(), document.getAuthor(),
                document.getSourceUrl(), document.getCreatedAt(),
                new DocumentDetails.Version(version.getId(), version.getNumber(), version.getText(),
                        version.getContentHash(), version.getCreatedAt()));
    }
}

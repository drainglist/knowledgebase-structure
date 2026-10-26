package com.polytech.knowledgebase.structure.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    Optional<DocumentVersion> findByDocumentIdAndNumber(UUID documentId, int number);
}

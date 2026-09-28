package com.polytech.knowledgebase.structure.repository;
import com.polytech.knowledgebase.structure.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface StatementRepository extends JpaRepository<Statement, String> {
    List<Statement> findByTopicIdAndStatusInOrderByIdAsc(String topicId, Collection<StatementStatus> statuses);
}

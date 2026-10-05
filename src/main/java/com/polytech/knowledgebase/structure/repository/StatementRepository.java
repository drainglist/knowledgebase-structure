package com.polytech.knowledgebase.structure.repository;

import com.polytech.knowledgebase.structure.domain.Statement;
import com.polytech.knowledgebase.structure.domain.StatementCategory;
import com.polytech.knowledgebase.structure.domain.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StatementRepository extends JpaRepository<Statement, String>, JpaSpecificationExecutor<Statement> {
    List<Statement> findByTopicIdAndStatusInOrderByIdAsc(String topicId, Collection<StatementStatus> statuses);

    @Query("""
            select distinct statement from Statement statement
            left join statement.referencedTopics reference
            where statement.status in :statuses
                and statement.category in :categories
                and (statement.topic.id in :primaryTopicIds or reference.id in :referenceTopicIds)
            order by statement.id
            """)
    List<Statement> findSearchCandidates(
            @Param("primaryTopicIds") Collection<String> primaryTopicIds,
            @Param("referenceTopicIds") Collection<String> referenceTopicIds,
            @Param("statuses") Collection<StatementStatus> statuses,
            @Param("categories") Collection<StatementCategory> categories);
}

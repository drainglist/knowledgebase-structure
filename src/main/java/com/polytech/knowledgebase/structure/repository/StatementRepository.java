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

    /**
     * Flat projection of the search candidates: no statement text, condition text or evidence
     * fragments are transferred, so a broad topic does not pull whole object graphs into memory.
     * One row per statement condition x referenced topic pair.
     */
    @Query("""
            select s.id, s.topic.id, s.topic.type, s.status, s.category, c.kind, c.minValue, c.maxValue, r.id
            from Statement s
            left join s.conditions c
            left join s.referencedTopics r
            where s.status in :statuses
                and s.category in :categories
                and (s.topic.id in :primaryTopicIds
                    or s.id in (select mentioned.id from Statement mentioned
                                join mentioned.referencedTopics linked where linked.id in :referenceTopicIds))
            order by s.id
            """)
    List<Object[]> findSearchProjection(
            @Param("primaryTopicIds") Collection<String> primaryTopicIds,
            @Param("referenceTopicIds") Collection<String> referenceTopicIds,
            @Param("statuses") Collection<StatementStatus> statuses,
            @Param("categories") Collection<StatementCategory> categories);

    /** Loads complete objects for a single result page only. */
    @Query("""
            select distinct s from Statement s
            left join fetch s.topic
            left join fetch s.conditions
            left join fetch s.referencedTopics
            where s.id in :ids
            """)
    List<Statement> findPageWithDetails(@Param("ids") Collection<String> ids);

    @Query("select s.status, count(s) from Statement s group by s.status")
    List<Object[]> countGroupedByStatus();
}
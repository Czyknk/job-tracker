package com.example.job_tracker.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    // JPQL w stringu: pakiet document nie importuje klas z jobapplication (zależność tylko w jedną stronę).
    @Query("select count(a) from JobApplication a join a.documents d where d.id = :documentId")
    long countApplicationsUsing(@Param("documentId") Long documentId);
}

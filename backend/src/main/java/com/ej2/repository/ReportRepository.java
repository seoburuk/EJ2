package com.ej2.repository;

import com.ej2.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Check if duplicate report exists
     */
    boolean existsByReporterIdAndReportTypeAndEntityId(Long reporterId, String reportType, Long entityId);

    /**
     * Find all reports for a specific entity
     */
    List<Report> findByReportTypeAndEntityId(String reportType, Long entityId);

    /**
     * Find user's submitted reports
     */
    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
}

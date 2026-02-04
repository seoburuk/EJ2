package com.ej2.mapper;

import com.ej2.dto.ReportDTO;
import com.ej2.dto.ReportDetailDTO;
import com.ej2.dto.ReportStatsDTO;
import com.ej2.dto.ReportSearchCriteria;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    /**
     * Get report statistics for dashboard
     */
    ReportStatsDTO selectReportStats();

    /**
     * Search reports with dynamic filters and pagination
     */
    List<ReportDTO> selectReports(
        @Param("criteria") ReportSearchCriteria criteria,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    /**
     * Count reports with filters (for pagination)
     */
    int countReports(@Param("criteria") ReportSearchCriteria criteria);

    /**
     * Get report detail with entity context
     */
    ReportDetailDTO selectReportDetail(@Param("reportId") Long reportId);
}

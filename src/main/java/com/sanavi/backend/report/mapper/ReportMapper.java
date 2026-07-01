package com.sanavi.backend.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.report.dto.Report;

@Mapper
public interface ReportMapper {
    int insertReport(Report report);

    int countDuplicateReport(
            @Param("reportUserId") String reportUserId,
            @Param("targetType") String targetType,
            @Param("targetId") String targetId);

    String selectBoardOwner(@Param("targetId") int targetId);

    String selectBoardCommentOwner(@Param("targetId") int targetId);

    String selectMatchOwner(@Param("targetId") int targetId);

    int increaseBoardReportCount(@Param("targetId") int targetId);

    int increaseBoardCommentReportCount(@Param("targetId") int targetId);

    int increaseMatchReportCount(@Param("targetId") int targetId);
}

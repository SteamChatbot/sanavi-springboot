package com.sanavi.backend.report.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.sanavi.backend.report.dto.Report;

@Mapper
public interface ReportMapper {
    void insertReport(Report report);
}

package com.sky.service;

import com.sky.vo.TurnoverReportVO;

import java.time.LocalDate;

public interface ReportService {
    /**
     * 统计每日营业额
     *
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getDailyTurnover(LocalDate begin, LocalDate end);
}

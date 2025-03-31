package com.sky.service;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

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

    /**
     * 统计每日新增用户数据
     *
     * @param begin
     * @param end
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);
}

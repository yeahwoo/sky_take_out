package com.sky.service.impl;

import com.sky.dto.TurnoverDTO;
import com.sky.entity.Orders;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    ReportMapper reportMapper;

    /**
     * 统计每日营业额
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getDailyTurnover(LocalDate begin, LocalDate end) {
        // 用LinkedHashMap让日期有序
        Map<LocalDate, BigDecimal> map = new LinkedHashMap<>();
        // 计算日期
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            map.put(date, BigDecimal.ZERO);
        }
        // 查询数据并写入对应的营业额
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("status", Orders.COMPLETED);
        parameter.put("begin", begin);
        parameter.put("end", end);
        List<TurnoverDTO> turnoverDTOList = reportMapper.getDailyTurnover(parameter);
        for(TurnoverDTO turnover : turnoverDTOList){
            map.put(turnover.getDate(),turnover.getTurnover());
        }
        // 封装VO返回
        String dateListStr = StringUtils.join(map.keySet(), ",");
        String turnoverListStr = StringUtils.join(map.values(), ",");
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }
}

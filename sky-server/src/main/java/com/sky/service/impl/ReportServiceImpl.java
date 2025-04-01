package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrderReportDTO;
import com.sky.dto.TurnoverDTO;
import com.sky.dto.UserCountDTO;
import com.sky.entity.Orders;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
        // 查询数据并写入对应的营业额
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("status", Orders.COMPLETED);
        parameter.put("begin", begin);
        parameter.put("end", end);
        List<TurnoverDTO> turnoverDTOList = reportMapper.getDailyTurnover(parameter);
        // 解析数据
        List<LocalDate> dateList = new ArrayList<>();
        List<BigDecimal> turnoverList = new ArrayList<>();
        turnoverDTOList.forEach(turnoverDTO -> {
            dateList.add(turnoverDTO.getDate());
            turnoverList.add(turnoverDTO.getTurnover());
        });
        // 封装VO返回
        String dateListStr = StringUtils.join(dateList, ",");
        String turnoverListStr = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }

    /**
     * 统计新增用户数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 查询
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("begin", begin);
        parameter.put("end", end);
        List<UserCountDTO> userCountDTOList = reportMapper.getUserStatistics(parameter);
        // 解析数据
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        userCountDTOList.forEach(userCountDTO -> {
            dateList.add(userCountDTO.getDate());
            newUserList.add(userCountDTO.getNewUser());
            totalUserList.add(userCountDTO.getTotalUser());
        });
        // 封装VO返回
        String dateListStr = StringUtils.join(dateList, ",");
        String newUserListStr = StringUtils.join(newUserList, ",");
        String totalUserListStr = StringUtils.join(totalUserList, ",");
        return UserReportVO.builder()
                .dateList(dateListStr)
                .newUserList(newUserListStr)
                .totalUserList(totalUserListStr)
                .build();
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 查询
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("begin", begin);
        parameter.put("end", end);
        List<OrderReportDTO> totalOrderReportDTO = reportMapper.getOrderStatistics(parameter);
        parameter.put("status", Orders.COMPLETED);
        List<OrderReportDTO> validOrderReportDTO = reportMapper.getOrderStatistics(parameter);
        // 解析数据
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> totalOrderList = new ArrayList<>();
        List<Integer> validOrderList = new ArrayList<>();
        totalOrderReportDTO.forEach(totalOrderDTO -> {
            dateList.add(totalOrderDTO.getDate());
            totalOrderList.add(totalOrderDTO.getOrderCount());
        });
        validOrderReportDTO.forEach(validOrderDTO -> {
            validOrderList.add(validOrderDTO.getOrderCount());
        });
        // 计算总数和完成率
        //时间区间内的总订单数
        int totalOrderCount = totalOrderList.stream().reduce(0, Integer::sum);
        //时间区间内的总有效订单数
        Integer validOrderCount = validOrderList.stream().reduce(0, Integer::sum);
        //订单完成率
        double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        // 封装VO返回
        String dateListStr = StringUtils.join(dateList, ",");
        String totalOrderListStr = StringUtils.join(totalOrderList, ",");
        String validOrderListStr = StringUtils.join(validOrderList, ",");
        return OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(totalOrderListStr)
                .validOrderCountList(validOrderListStr)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        // 查询
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("begin", begin);
        parameter.put("end", end);
        List<GoodsSalesDTO> goodsSalesDTO = reportMapper.getTop10(parameter);
        // 解析
        String nameList = StringUtils.join(goodsSalesDTO.stream()
                .map(GoodsSalesDTO::getName).collect(Collectors.toList()), ",");
        String numberList = StringUtils.join(goodsSalesDTO.stream()
                .map(GoodsSalesDTO::getNumber).collect(Collectors.toList()), ",");
        // 封装
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}

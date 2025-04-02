package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrderReportDTO;
import com.sky.dto.TurnoverDTO;
import com.sky.dto.UserCountDTO;
import com.sky.entity.Orders;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    ReportMapper reportMapper;
    @Autowired
    WorkspaceService workspaceService;

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

    /**
     * 导出近30天的数据
     *
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 首先查询出相关的数据
        LocalDateTime begin = LocalDateTime.of(LocalDate.now().minusDays(30), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDate.now().plusDays(-1), LocalTime.MAX);
        BusinessDataVO businessData = workspaceService.getBusinessData(begin, end);
        // 将excel模板读入内存
        // 这里用类加载器读取文件，当项目打包部署时所有类和resource目录下的资源会被拷贝到classpath下
        // 如果用普通的基于文件系统路径的方式读取文件，当项目部署后就无法正确读取到文件了
        InputStream input = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        if (input == null) {
            log.error("文件不存在");
            return;
        }
        // 利用模板创建excel处理类
        try {
            // 创建excel处理类
            XSSFWorkbook excel = new XSSFWorkbook(input);
            // 获取第一个sheet
            XSSFSheet sheet = excel.getSheet("Sheet1");
            // 将数据写入excel文件
            // 第二行第二列写入日期
            sheet.getRow(1).getCell(1)
                    .setCellValue(begin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                            " 至 " +
                            end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    );
            // 第4行第3列写入营业额
            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            // 第4行第5列写入订单完成率
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            // 第4行第7列写入新增用户数
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());
            // 第5行第3列写入有效订单数
            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            // 第5行第5列写入平均客单价
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());
            // 第8行开始循环写入本月明细数据
            // 2：日期
            // 3：营业额
            // 4：有效订单
            // 5：订单完成率
            // 6：平均客单价
            // 7：新增用户数
            XSSFRow row;
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i).toLocalDate();
                //准备明细数据
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            // 将文件写入响应流中
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=report.xlsx");
            ServletOutputStream output = response.getOutputStream();
            excel.write(output);
            // 释放资源
            output.flush();
            output.close();
            excel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

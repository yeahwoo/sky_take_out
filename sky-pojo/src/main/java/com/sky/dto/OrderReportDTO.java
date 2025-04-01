package com.sky.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderReportDTO {
    private LocalDate date;
    private Integer orderCount;
}

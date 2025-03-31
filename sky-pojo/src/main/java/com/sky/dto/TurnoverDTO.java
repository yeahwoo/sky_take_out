package com.sky.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TurnoverDTO {
    private LocalDate date;
    private BigDecimal turnover;
}

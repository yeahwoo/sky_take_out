package com.sky.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserCountDTO {
    private LocalDate date;
    private Integer newUser;
    private Integer totalUser;
}

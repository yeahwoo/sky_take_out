package com.sky.mapper;

import com.sky.dto.TurnoverDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {
    List<TurnoverDTO> getDailyTurnover(Map<String, Object> parameter);
}

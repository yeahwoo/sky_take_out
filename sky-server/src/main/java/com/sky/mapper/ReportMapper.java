package com.sky.mapper;

import com.sky.dto.TurnoverDTO;
import com.sky.dto.UserCountDTO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {
    /**
     * 每日营业额统计
     *
     * @param parameter
     * @return
     */
    List<TurnoverDTO> getDailyTurnover(Map<String, Object> parameter);

    /**
     * 新增用户统计
     *
     * @param parameter
     * @return
     */
    List<UserCountDTO> getUserStatistics(Map<String, Object> parameter);
}

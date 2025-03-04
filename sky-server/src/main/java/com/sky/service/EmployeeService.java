package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     *
     * @param employeeDTO
     *
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
     PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 设置员工账号状态
     * @param status
     * @param id
     */
     void setStatus(Integer status, Long id);

    /**
     * 根据用户id查询员工信息
     * @param id
     * @return
     */
     Employee getById(Long id);

    /**
     * 修改员工信息
     * @param employeeDTO
     */
    void updateEmployee(EmployeeDTO employeeDTO);

}

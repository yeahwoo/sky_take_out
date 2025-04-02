package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrdersMapper {
    /**
     * 插入订单数据并生成订单号
     *
     * @param order
     */
    void insert(Orders order);

    /**
     * 批量插入订单详情数据
     *
     * @param orderDetailList
     */
    void insertOrderDetail(List<OrderDetail> orderDetailList);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     *
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页订单查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     *
     * @param orderId
     * @return
     */
    @Select("select * from orders where id = #{orderId}")
    Orders getById(Long orderId);

    /**
     * 根据订单状态统计订单数量
     *
     * @param toBeConfirmed
     * @return
     */
    @Select("select count(*) from orders where status = #{status}")
    Integer countByStatus(Integer toBeConfirmed);

    /**
     * 根据订单状态和下单时间查询订单
     *
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getOrderEarlier(Integer status, LocalDateTime orderTime);

    /**
     * 计算订单数量
     *
     * @param map
     * @return
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 统计营业额
     *
     * @param map
     * @return
     */
    Double sumByMap(Map<String, Object> map);
}

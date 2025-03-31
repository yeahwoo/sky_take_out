package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 定时任务
 * Component注解注册为Bean
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrdersMapper ordersMapper;

    /**
     * 处理超时订单，每分钟处理一次
     */
    // TODO:超时订单可以引入消息队列优化
    @Scheduled(cron = "0 * * * * ?")
    // @Scheduled(cron = "1/3 * * * * ?") // 测试代码
    public void timeOutProcess() {
        log.info("处理超时未支付订单:{}", new Date());
        // 查找订单支付状态为待支付且下单时间早于15分钟前的订单
        LocalDateTime orderTime = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = ordersMapper.getOrderEarlier(Orders.PENDING_PAYMENT, orderTime);
        // 将这些订单的状态全部改为取消
        if (!ordersList.isEmpty()) {
            for (Orders order : ordersList) {
                order.setStatus(Orders.CANCELLED);
                // 补充取消原因
                order.setCancelReason("订单超时未支付，自动取消");
                order.setCancelTime(LocalDateTime.now());
                // 更新订单
                ordersMapper.update(order);
            }
        }
    }

    /**
     * 处理未完成订单，每天凌晨一点集中处理
     */
    @Scheduled(cron = "0 0 1 * * ?")
    // @Scheduled(cron = "0/8 * * * * ?") // 测试代码
    public void incompleteProcess() {
        log.info("处理未完成订单:{}", new Date());
        // 查找配送状态为派送中，并且下单时间早于1小时前的订单（即前一天的订单）
        LocalDateTime orderTime = LocalDateTime.now().plusHours(-1);
        List<Orders> ordersList = ordersMapper.getOrderEarlier(Orders.DELIVERY_IN_PROGRESS, orderTime);
        // 将订单状态都改为完成
        if (!ordersList.isEmpty()) {
            for (Orders order : ordersList) {
                order.setStatus(Orders.COMPLETED);
                order.setDeliveryTime(LocalDateTime.now());
                // 更新订单
                ordersMapper.update(order);
            }
        }
    }
}

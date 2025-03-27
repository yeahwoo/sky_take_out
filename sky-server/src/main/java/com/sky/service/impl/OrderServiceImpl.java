package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.CartMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private OrdersMapper ordersMapper;

    /**
     * 用户提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    // 涉及到多表插入，因此需要开启事务
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        // 首先从提交数据中拿到收获地址
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        // 判断收货地址是否为空，空则直接抛出异常（前端已经判断，这里做双重保险）
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 根据用户id去购物车查询菜品或者套餐
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = cartMapper.list(shoppingCart);
        // 如果查出来为空则抛出异常
        if(shoppingCartList.isEmpty()){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 构造订单实体并将其插入数据库获取订单id
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        // 设置订单号
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        // 设置订单状态未付款
        order.setStatus(Orders.PENDING_PAYMENT);
        // 设置账单状态未支付
        order.setPayStatus(Orders.UN_PAID);
        // 设置下单时间
        order.setOrderTime(LocalDateTime.now());
        // 设置收货人
        order.setConsignee(addressBook.getConsignee());
        // 设置用户id
        order.setUserId(userId);
        // 设置电话号码
        order.setPhone(addressBook.getPhone());
        // 设置详细收获地址
        order.setAddress(addressBook.getDetail());
        // 插入
        ordersMapper.insert(order);

        // 根据购物车查出来的商品构造订单明细，并绑定订单id，批量插入数据库
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart cart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        ordersMapper.insertOrderDetail(orderDetailList);
        // 清空购物车里的所有商品
        cartMapper.delete(shoppingCart);
        // 这里orderSubmitVO的订单号、金额字段与order不一致，因此不能直接用BeanUtils拷贝
        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderAmount(order.getAmount())
                .orderNumber(order.getNumber())
                .orderTime(order.getOrderTime())
                .build();
    }
}

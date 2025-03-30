package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    // 微信支付工具类
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    // 高德地图配置属性
    @Value("${sky.amap.key}")
    private String key;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.amap.geoCodeUrl}")
    private String geoCodeUrl;
    @Value("${sky.amap.directionUrl}")
    private String directionUrl;

    /**
     * 用户提交订单
     *
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
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 判断地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName(),
                addressBook.getDistrictName() +
                        addressBook.getDetail());

        // 根据用户id去购物车查询菜品或者套餐
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = cartMapper.list(shoppingCart);
        // 如果查出来为空则抛出异常
        if (shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 构造订单实体并将其插入数据库获取订单id
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
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
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
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

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal("0.01"), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("FAIL")) {
            throw new OrderBusinessException("支付失败");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = ordersMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        ordersMapper.update(orders);
    }

    /**
     * 历史订单查询
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int page, int pageSize, Integer status) {
        // 首先利用PageHelper初始化
        PageHelper.startPage(page, pageSize);

        // 根据当前用户的id和订单状态查询订单
        Long userId = BaseContext.getCurrentId();
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> pageOrders = ordersMapper.pageQuery(ordersPageQueryDTO);

        // 将查到的单页数据封装成VO
        List<OrderVO> orderVOList = getOrderVOList(pageOrders);
        // 封装成PageResult返回
        return new PageResult(pageOrders.getTotal(), orderVOList);
    }

    /**
     * 根据订单id查询订单详情
     *
     * @param orderId
     * @return
     */
    public OrderVO orderDetail(Long orderId) {
        // 根据订单id查询订单详情列表
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

        // 封装成VO
        OrderVO orderVO = new OrderVO();
        // 查询订单信息补充VO
        Orders orders = ordersMapper.getById(orderId);
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        // 返回
        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param orderId
     */
    @Transactional
    public void cancelOrder4User(Long orderId) throws Exception {
        // 首先根据id查询订单
        Orders orders = ordersMapper.getById(orderId);

        // 如果订单不存在直接返回
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 如果订单存在判断是否可以取消（订单状态是否大于2），如果不可以取消抛出业务异常
        if (orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 如果是待接单状态则要进行退款，调用微信工具退款接口
        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            refund(orders);
        }

        // 待付款和待接单都需要修改订单状态为取消订单
        orders.setStatus(Orders.CANCELLED);

        // 更新该订单的退款相关的字段
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        ordersMapper.update(orders);

    }

    /**
     * 重新下单
     *
     * @param id
     */
    @Override
    @Transactional
    public void reOrder(Long id) {
        // 根据订单id查询出订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 根据订单详情列表将其转换为购物车商品
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(orderDetail -> {
            // 将每一个订单详情（一个dish或者一个套餐）装到购物车实体中
            ShoppingCart shoppingCart = new ShoppingCart();
            // 忽略id字段（该字段不拷贝）
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            // 补充用户id
            shoppingCart.setUserId(BaseContext.getCurrentId());
            // 补充创建时间（由于是批量插入，参数是List，所以无法调用set方法）
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList()); // 收集成List

        // 将购物车实体重新插入
        cartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    public void delivery(Long id) {
        // 将订单状态修改为配送中
        // 利用id查出订单实体
        Orders order = ordersMapper.getById(id);
        // 判空
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 判段订单是否已接单，接单了才能派送
        if (!order.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 修改订单的状态
        // 这里创建一个新的实体只用到一个字段
        // 如果直接在原来的order上其他的字段也会被用到（可能会用到旧信息）
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        // 调用update方法修改状态
        ordersMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void complete(Long id) {
        // 将订单状态修改为配送中
        // 利用id查出订单实体
        Orders order = ordersMapper.getById(id);
        // 判空
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 判段订单是否已派送，派送的订单才能完结
        if (!order.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 修改订单的状态
        // 这里创建一个新的实体只用到一个字段
        // 如果直接在原来的order上其他的字段也会被用到（可能会用到旧信息）
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();

        // 调用update方法修改状态
        ordersMapper.update(orders);
    }

    /**
     * 订单条件查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery4Admin(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 首先进行分页设置
        int pageNum = ordersPageQueryDTO.getPage();
        int pageSize = ordersPageQueryDTO.getPageSize();
        PageHelper.startPage(pageNum, pageSize);
        // 条件查询出订单列表
        Page<Orders> pageOrders = ordersMapper.pageQuery(ordersPageQueryDTO);
        // 转为VO
        List<OrderVO> orderVOList = getOrderVOList(pageOrders);
        // 封装成PageResult返回
        return new PageResult(pageOrders.getTotal(), orderVOList);
    }

    /**
     * 订单统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 分别查询出待接单、待派单、派送中的订单数量
        Integer toBeConfirmed = ordersMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = ordersMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = ordersMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        // 封装到OrderStatisticsVO中
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 根据id查询订单
        Orders orders = ordersMapper.getById(ordersConfirmDTO.getId());
        // 判空
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 判断状态
        if (!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.CONFIRMED);
        // 修改订单状态
        ordersMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Transactional
    @Override
    public void reject(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 查询指定订单
        Orders order = ordersMapper.getById(ordersRejectionDTO.getId());
        // 判空
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 判断状态
        if (!order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 如果订单已经支付还需要退款
        if (order.getPayStatus().equals(Orders.PAID)) {
            refund(order);
        }

        // 修改状态和取消原因
        Orders newOrder = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        // 更新
        ordersMapper.update(newOrder);
    }

    /**
     * 商家取消订单
     *
     * @param ordersCancelDTO
     * @throws Exception
     */
    @Transactional
    @Override
    public void cancelOrder4Admin(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 查询指定订单
        Orders order = ordersMapper.getById(ordersCancelDTO.getId());
        // 判空
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 如果订单已经支付还需要退款
        if (order.getPayStatus().equals(Orders.PAID)) {
            refund(order);
        }

        // 修改状态和取消原因
        Orders newOrder = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        // 更新
        ordersMapper.update(newOrder);

    }

    /**
     * 将订单列表转为VO列表
     *
     * @param pageOrders
     * @return
     */
    private List<OrderVO> getOrderVOList(Page<Orders> pageOrders) {
        List<OrderVO> orderVOList = new ArrayList<>();
        if (!pageOrders.isEmpty()) {
            for (Orders orders : pageOrders) {
                // 将每个order的数据封装成VO（VO是继承自实体的，所有字段都有，所以可以直接copy）
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                // 查询OrderDetail，完善VO
                List<OrderDetail> orderDetail = orderDetailMapper.getByOrderId(orders.getId());
                orderVO.setOrderDetailList(orderDetail);
                // 查询菜品信息
                String orderDishes = getOrderDishesStr(orders.getId());
                orderVO.setOrderDishes(orderDishes);
                // 添加到VO列表中
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 获取订单菜品信息
     *
     * @param id
     * @return
     */
    private String getOrderDishesStr(Long id) {
        // 首先根据id查出订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        // 提取将每一道菜的名称和数量拼接成字符串
        StringBuilder orderDishes = new StringBuilder();
        orderDetailList.forEach(orderDetail -> {
            // 获取名称
            String name = orderDetail.getName();
            // 获取数量
            Integer number = orderDetail.getNumber();
            // 拼接
            orderDishes.append(name).append("*").append(number).append(";");
        });
        // 返回
        return orderDishes.toString();
    }

    /**
     * 退款
     *
     * @param orders
     * @throws Exception
     */
    private void refund(Orders orders) throws Exception {
        String response = weChatPayUtil.myRefund(
                orders.getNumber(), //商户订单号
                orders.getNumber(), //商户退款单号
                // orders.getAmount(),//退款金额，单位 元
                // 退款金额这里先写死
                new BigDecimal("0.01"),//退款金额，单位 元
                new BigDecimal("0.01")//原订单金额
        );
        JSONObject jsonObject = JSONObject.parseObject(response);
        String code = jsonObject.getString("code");
        String message = jsonObject.getString("message");
        log.info("微信退款接口返回信息：{}", response);
        if (code == null)
            throw new OrderBusinessException(MessageConstant.REFUND_ERROR);
        if (code.equals("FAIL"))
            throw new OrderBusinessException(message);
        // 退款的订单要修改支付状态为退款
        orders.setPayStatus(Orders.REFUND);
    }

    /**
     * 判断配送距离是否超出范围
     *
     * @param targetAddress
     */
    private void checkOutOfRange(String city, String targetAddress) {
        /* 校验是否在同一个城市 */
        if (!city.equals("南京市"))
            throw new OrderBusinessException("超出配送范围");
        /* 地址编码 */
        // 封装请求参数
        Map<String, String> map = new HashMap<>();
        map.put("key", key);
        map.put("address", shopAddress);
        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet(geoCodeUrl, map);
        //数据解析
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("1")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }
        JSONArray geocodes = jsonObject.getJSONArray("geocodes");
        String shopLocation = geocodes.getJSONObject(0).getString("location");

        //获取用户的经纬度坐标
        map.put("address", city + targetAddress);
        String userCoordinate = HttpClientUtil.doGet(geoCodeUrl, map);
        //数据解析
        jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("1")) {
            throw new OrderBusinessException("用户地址解析失败");
        }
        geocodes = jsonObject.getJSONArray("geocodes");
        String userLocation = geocodes.getJSONObject(0).getString("location");

        /* 路径规划 */
        // 封装请求参数
        map = new HashMap<>();
        map.put("key", key);
        map.put("origin", shopLocation);
        map.put("destination", userLocation);
        String route = HttpClientUtil.doGet(directionUrl, map);
        // 数据解析
        jsonObject = JSON.parseObject(route);
        if (!jsonObject.getString("status").equals("1")) {
            throw new OrderBusinessException("路线解析失败");
        }
        JSONArray path = jsonObject.getJSONObject("route").getJSONArray("paths");
        int distance = Integer.parseInt(path.getJSONObject(0).getString("distance"));
        log.info("配送距离：{}米", distance);
        if (distance > 5000) {
            throw new OrderBusinessException("超出配送距离");
        }
    }

}

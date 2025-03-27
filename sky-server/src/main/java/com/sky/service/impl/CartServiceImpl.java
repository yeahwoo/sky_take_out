package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.CartMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.CartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    public void add(ShoppingCartDTO shoppingCartDTO) {
        // 获取用户id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(userId);

        // 首先查询购物车中该菜品或者套餐是否已经存在
        List<ShoppingCart> shoppingCartList = cartMapper.list(shoppingCart);

        // 如果已经存在，那么数量加1即可
        if (!shoppingCartList.isEmpty()) {
            // 这里只涉及一家店铺，一个用户只会有一个购物车，因此根据用户id和dishId或者setmealId只能查到一个结果
            ShoppingCart cart = shoppingCartList.get(0);
            // 将数量加1
            cart.setNumber(cart.getNumber() + 1);
            // 更新
            cartMapper.update(cart);
        } else {
            // 否则就是空的，创建新的实体并插入数据库
            // 构建一个实体
            // 判断是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                // 菜品
                Dish dish = dishMapper.listByIds(List.of(dishId)).get(0);
                shoppingCart.setNumber(1);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                // 套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setNumber(1);
            }
            // 将数据插入到数据库中
            cartMapper.insert(shoppingCart);
        }
    }

    public List<ShoppingCart> listCart() {
        // 根据用户id查询购物车
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        return cartMapper.list(shoppingCart);
    }

    public void cleanCart() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        cartMapper.delete(shoppingCart);
    }

    public void subCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(userId);
        // 判断当前商品的数量是否为1
        // 如果不为1则设置数量减1
        // 否则删除该条数据
        shoppingCart = cartMapper.list(shoppingCart).get(0);
        Integer number = shoppingCart.getNumber();
        if (number > 1) {
            shoppingCart.setNumber(number - 1);
            cartMapper.update(shoppingCart);
        } else {
            cartMapper.delete(shoppingCart);
        }
    }
}

package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.ShoppingCart;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CartMapper {
    /**
     * 批量查询购物车
     *
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 插入购物车
     *
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart (name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time) " +
            " values (#{name},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{image},#{createTime})")
    @AutoFill(OperationType.INSERT)
    void insert(ShoppingCart shoppingCart);

    /**
     * 更新购物车
     *
     * @param shoppingCart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    @AutoFill(OperationType.UPDATE)
    void update(ShoppingCart shoppingCart);

    /**
     * 删除购物车中的商品
     *
     * @param shoppingCart
     */
    void delete(ShoppingCart shoppingCart);

    /**
     * 批量加入购物车
     * @param shoppingCartList
     */
    // 这里用不了AutoFill注解，因为插入的参数是List类型，没办法获得set方法，还是放在service中设置
    void insertBatch(List<ShoppingCart> shoppingCartList);
}

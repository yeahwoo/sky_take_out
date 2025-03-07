package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    @Transactional
    public void saveDishWithFlavor(DishDTO dishDTO) {
        // 首先创建Dish并将数据拷贝进去
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 将dish保存到菜品表中
        // 由于dish的id是自增的并非由用户设置，而是由数据库插入后自动生成
        // 因此要在mapper的映射文件中先开启插入后返回主键才能获取id
        dishMapper.insert(dish);

        // 从dishDTO中拿到菜品id
        Long dishId = dish.getId();

        // 从DishDTO中获取DishFlavor列表
        List<DishFlavor> flavors = dishDTO.getFlavors();

        // 利用菜品id将口味列表插入到口味表中
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 获取参数让PageHelper初始化
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 调用Mapper函数去查询数据并用PageHelper封装成VO数据
        Page<DishVO> dishVOPage = dishMapper.pageQuery(dishPageQueryDTO);
        // 把查到的数据返回
        return new PageResult(dishVOPage.getTotal(), dishVOPage.getResult());
    }

    @Transactional
    public void deleteDishBatch(List<Long> ids) {
        // 首先要判断菜品能不能删除
        // 起售中的菜品不能删除（查status）

        // 批量查询dish表，查出指定id的所有菜品的status，然后判断
        List<Dish> dishes = dishMapper.listByIds(ids);
        for (Dish dish : dishes) {
            if (dish.getStatus() == 1)
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }

        // 被套餐关连的套餐不能删（查setmeal_dish）
        // 查出来看是不是null
        List<Dish> setmealDishes = setmealDishMapper.listByIds(ids);
        if (!setmealDishes.isEmpty())
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);

        // 可以删除还需要把口味一并删除
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByIds(ids);

    }
}

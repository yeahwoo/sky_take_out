package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
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

import java.util.ArrayList;
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
        List<SetmealDish> setmealDishes = setmealDishMapper.listByIds(ids);
        if (!setmealDishes.isEmpty())
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);

        // 可以删除还需要把口味一并删除
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByIds(ids);

    }

    public DishVO getByIdWithFlavor(Long id) {
        DishVO dishVO = new DishVO();
        // 首先根据id查出菜品
        List<Dish> dishList = dishMapper.listByIds(List.of(id));
        if (dishList.isEmpty()) return null;
        Dish dish = dishList.get(0);
        // 将其封装到DishVO中
        BeanUtils.copyProperties(dish, dishVO);
        // 再根据id查口味并封装
        List<DishFlavor> dishFlavor = dishFlavorMapper.getById(id);
        dishVO.setFlavors(dishFlavor);
        return dishVO;
    }

    @Transactional
    public void updateDishWithFlavor(DishDTO dishDTO) {
        // 首先将dishDTO封装到dish中
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 更新dish表
        dishMapper.update(dish);
        // 删除后再插入新的（先判空）
        // 先删掉
        Long dishId = dishDTO.getId();
        dishFlavorMapper.deleteByIds(List.of(dishId));
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        if (!dishFlavors.isEmpty()) {
            // 再插入
            // 由于DishDTO传过来的flavor中dishId是空的，因此需要手动设置
            for (DishFlavor dishFlavor : dishFlavors) {
                dishFlavor.setDishId(dishId);
            }
            dishFlavorMapper.insertBatch(dishFlavors);
        }
    }

    public List<Dish> listByCategoryId(Long categoryId) {
        return dishMapper.listByCategoryId(categoryId);
    }

    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getById(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    public void updateStatus(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }
}

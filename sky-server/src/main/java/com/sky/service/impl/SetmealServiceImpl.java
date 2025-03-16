package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    @Transactional
    public void save(SetmealDTO setmealDTO) {
        // 从dto中拿到数据封装到实体中
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 补充状态字段默认为停售
        setmeal.setStatus(StatusConstant.DISABLE);
        // 调用mapper新增套餐，拿到套餐id
        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();
        // 套餐关联到套餐关系表（新增当前套餐与套餐中所有菜品的映射）
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        // 把套餐id加进去
        if (!setmealDishList.isEmpty()) {
            for (SetmealDish setmealDish : setmealDishList) {
                setmealDish.setSetmealId(setmealId);
            }
        }
        setmealDishMapper.insertBatch(setmealDishList);
    }

    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 初始化PageHelper
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        // 查询数据存到Page中
        Page<SetmealVO> setmealVOPage = setmealMapper.pageQuery(setmealPageQueryDTO);
        // 返回结果
        return new PageResult(setmealVOPage.getTotal(), setmealVOPage.getResult());
    }

    @Transactional
    public void delete(List<Long> ids) {
        // 首先查套餐状态，如果是起售中的套餐则抛出异常
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        // 如果可以删除，则先去关系表中将该条数据删掉
        setmealDishMapper.deleteBySetmealIds(ids);
        // 然后从套餐表中删除该条数据
        setmealMapper.deleteByIds(ids);
    }

    @Transactional
    public SetmealVO getById(Long id) {
        // 用id查Setmeal，然后封装到VO中
        SetmealVO setmealVO = new SetmealVO();
        Setmeal setmeal = setmealMapper.getById(id);
        BeanUtils.copyProperties(setmeal, setmealVO);
        // 查询套餐中的菜品补充VO实体
        List<SetmealDish> setmealDishes = setmealDishMapper.listByIds(List.of(setmeal.getId()));
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Transactional
    public void update(SetmealDTO setmealDTO) {
        // 将dto封装成实体
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 直接调用mapper方法修改
        setmealMapper.update(setmeal);
        // 由于Setmeal没有套餐关系的字段，因此关系字段需要手动修改关系表
        // 因此每次修改之前先删掉关系列表，然后重新插入

        // 首先获取id
        Long setmealId = setmealDTO.getId();
        // 去关系表中根据套餐id删除数据
        setmealDishMapper.deleteBySetmealIds(List.of(setmealId));
        // 查询当前套餐，重新插入套餐id然后把关系重新插入
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishList) {
            setmealDish.setSetmealId(setmealId);
        }
        setmealDishMapper.insertBatch(setmealDishList);
    }

    @Transactional
    public void updateStatus(Integer status, Long id){
        // 如果要起售，首先要判断套餐中的菜品是否有停售的，如果停售则不可以起售套餐
        if(status == StatusConstant.ENABLE){
            // 首先根据id查到套餐里的菜品列表
            List<Dish> dishList = dishMapper.listBySetmealIds(id);
            if(!dishList.isEmpty()) {
                // 判断其中是否有停售的菜
                for (Dish dish : dishList) {
                    if (dish.getStatus() == StatusConstant.DISABLE) {
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                }
            }
        }
        // 如果通过校验或者是停售，直接调用方法
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }


}

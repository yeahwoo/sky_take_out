package com.sky.controller.admin;

import com.sky.constant.RedisKeyConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import com.sky.entity.Dish;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品相关接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 清理redis缓存
     *
     * @param key
     */
    private void cleanRedisCache(String key) {
        // 当数据库中的数据进行修改或删除时，清理redis缓存
        Set<String> keys = redisTemplate.keys(key);
        if (keys != null) {
            redisTemplate.delete(keys);
        }
    }

    @PostMapping
    @ApiOperation("新增菜品")
    public Result saveDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品:{}", dishDTO);
        dishService.saveDishWithFlavor(dishDTO);
        log.info("删除redis指定分类的缓存:{}", dishDTO.getCategoryId());
        cleanRedisCache(RedisKeyConstant.DISH_CATEGORY_PREFIX + dishDTO.getCategoryId());
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result deleteDish(@RequestParam List<Long> ids) {
        log.info("批量删除菜品:{}", ids);
        dishService.deleteDishBatch(ids);
        log.info("删除redis所有菜品缓存");
        cleanRedisCache("dish*");
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品:{}", id);
        // 这里要VO是因为页面展示时有Dish类中没有的字段
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品:{}", dishDTO);
        dishService.updateDishWithFlavor(dishDTO);
        Long categoryId = dishDTO.getCategoryId();
        log.info("删除redis指定分类的缓存:{}", categoryId);
        cleanRedisCache(RedisKeyConstant.DISH_CATEGORY_PREFIX + categoryId);
        return Result.success();
    }

    /**
     * 根据类别id查询类别中的所有菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询分类中的菜品")
    public Result<List<Dish>> listByCategoryId(Long categoryId) {
        log.info("根据类别id查询菜品:{}", categoryId);
        List<Dish> list = dishService.listByCategoryId(categoryId);
        return Result.success(list);
    }

    /**
     * 修改菜品状态
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("修改菜品状态")
    public Result updateDishStatus(@PathVariable Integer status, Long id) {
        log.info("修改菜品状态:{},id:{}", status, id);
        dishService.updateStatus(status, id);
        log.info("删除redis所有缓存");
        cleanRedisCache("dish*");
        return Result.success();
    }
}

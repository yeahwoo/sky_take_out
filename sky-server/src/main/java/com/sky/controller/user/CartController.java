package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.CartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("user/shoppingCart")
@Slf4j
@Api(tags = "C端-购物车相关接口")
public class CartController {
    @Autowired
    CartService cartService;

    /**
     * 购物车添加商品
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("购物车添加商品")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("购物车添加商品:{}",shoppingCartDTO);
        cartService.add(shoppingCartDTO);
        return Result.success();
    }


    /**
     * 查询购物车
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询购物车")
    public Result<List<ShoppingCart>> list(){
        log.info("查询购物车");
        return Result.success(cartService.listCart());
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车商品")
    public Result clean(){
        log.info("清空购物车");
        cartService.cleanCart();
        return Result.success();
    }

    @PostMapping("/sub")
    @ApiOperation("购物车商品减1")
    public Result subCart(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("购物车商品减1");
        cartService.subCart(shoppingCartDTO);
        return Result.success();
    }
}

package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);

        // 清理缓存数据--新建菜品时，受影响的缓存数据为该菜品对应的“菜品分类”数据，因此需要构造redis 的key为菜品分类名称
        String key = "dish" + dishDTO.getCategoryId();
        cleanCache(key);// 此时精确清理
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) { // 由于请求参数是Query的方式（地址栏加上？key=value），因此不需要在参数之前加@RequestBody注解
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageResult pageResult=dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }


    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids) {  // 加上@RequestParam注解后，可以由MVC框架动态的解析里面参数字符串
        log.info("菜品批量删除：{}",ids);
        dishService.deleteBatch(ids);

        // 删除菜品可能会影响到多个key，这里为了简便，清理所有以"dish_"开头的redis的key
        /// 获取dish_开头的redis key的集合
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) { // 由于需要查询的内容中包含菜品口味信息，所以用VO实体类
        log.info("根据id查询菜品：{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }


    /**
     * 修改菜品
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品：{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);
        // 问题：修改菜品的时候，是删除一个Redis缓存呢还是多个缓存，取决于修改菜品的什么属性。如果是价格名称等普通属性，那么删除的就是该菜品所对应的菜品分类的redis缓存；如果修改的是菜品的”菜品分类“属性，那么删除的便是多个菜品分类缓存
        /// 为了简捷（修改菜品这个操作也不是常规操作，频次较少），直接删除所有的缓存数据
        /// 删除菜品可能会影响到多个key，这里为了简便，清理所有以"dish_"开头的redis的key
        /// 获取dish_开头的redis key的集合
        cleanCache("dish");
        System.out.println("执行了cleanCache....");

        return Result.success();

    }


    /**
     *启用禁用菜品参数
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用菜品参数")
    // 如果参数使用了路径参数，则需要在参数前面加注解@PathVariable
    public Result startOrstop(@PathVariable("status") Integer status, Long id) {
        log.info("启用禁用菜品：{}，{}", status, id);
        dishService.startOrStop(status, id);

        /// 删除菜品可能会影响到多个key，这里为了简便，清理所有以"dish_"开头的redis的key
        cleanCache("dish_*");
        return Result.success();

    }
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }


    /**
     * 清理缓存数据  私有方法
     * @param pattern
     */
    private void cleanCache(String pattern) {
        /// 获取pattern模式开头的redis key的集合
        Set<String> keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}

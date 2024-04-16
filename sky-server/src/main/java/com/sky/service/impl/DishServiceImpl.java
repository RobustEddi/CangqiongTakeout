package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import jdk.net.SocketFlow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;
    /**
     * 新增菜品和对应的口味数据
     * @param dishDTO
     */

    @Transactional // 涉及到多个表的操作，为了保证操作的一致性（要么表操作全程跟，要么全失败），需要加事务注解
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish); // 注意dishDTO与dish属性命名需要一致才能拷贝
        // 向菜品表插入1条数据
        dishMapper.insert(dish);

        // 获取insert语句生成的主键值
        Long dishiId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor ->{
                // 遍历flavors数组，并给每个元素dishFlavor设置dishId进行标记
                dishFlavor.setDishId(dishiId);
            } );

            // 向口味表插入n条数据，flavors是由前端传来的一个数组
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 为了适应前端返回类型，这里泛型用DishVO
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        // 判断当前菜品是否能够删除--是否存在起售中的菜品？
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                //当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能够删除--是否被套餐关联了？
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            // 存在当前菜品被套餐关联情况，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的菜品数据
//        for (Long id : ids) {
//
//            dishMapper.deleteById(id);
//        }
        //删除菜品表中的菜品数据(利用动态sql优化后)
        dishMapper.deleteByIds(ids);

        //删除菜品关联的口味数据
//        for (Long id : ids) {
//            dishFlavorMapper.deleteFlavorById(id);
//        }
        //删除菜品关联的口味数据(利用动态sql优化后)
        dishFlavorMapper.deleteFlavorByIds(ids);
    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据id 查询菜品数据
        Dish dish = dishMapper.getById(id);

        //根据菜品id查询口味数据
        List<DishFlavor> dishFlavors=dishFlavorMapper.getByDishId(id);

        //将查询到的数据封装到VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 修改菜品基本信息
        dishMapper.update(dish);

        //修改口味基本信息
        // /删除原来口味
        dishFlavorMapper.deleteFlavorById(dish.getId());

        // /添加新口味

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dish.getId());
            });
        }
        dishFlavorMapper.insertBatch(flavors);
    }

    /**
     * 根据菜品id启用禁用菜品
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = dishMapper.getById(id);
        // 查看菜品关联套餐状态，
        Integer status_setmeal= setmealMapper.getSetmealStatus(id);
        // 如果套餐禁用、则可以禁用菜品，如果套餐没有禁用，则不能禁用菜品
        if (status_setmeal ==null || status_setmeal==0) {
            // 如果套餐没被启用，则可以更新菜品状态
            log.info("套餐没被启用，可以更新菜品状态");
            dish.setStatus(status);
            dishMapper.update(dish);


        } else {
            log.info("套餐被启用，无法更新菜品状态");
            // 存在当前菜品被套餐关联情况，不能删除
            throw new DeletionNotAllowedException("菜品关联套餐被启用，无法更新菜品状态");

        }






    }

    /**
     *根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)  // 筛选在售的菜品
                .build();
        // TODO:20240414
        return dishMapper.list(dish);
    }


}




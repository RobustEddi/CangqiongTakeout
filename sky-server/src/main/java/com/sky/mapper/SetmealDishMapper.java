package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据多个菜品id查询对应的套餐id
     * @param dishIds
     * @return
     */
    // select setmeal_id from setmeal_dish where dish_id in (1,2,3,4)
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);


    /**
     * 套餐菜品表插入套餐菜品关联数据
     * @param setmealDish
     */
    void insert(SetmealDish setmealDish);

    /**
     * 根据套餐id删除套餐关联菜品
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id=#{setmealId}")
    void deleteBySetmealId(Long setmealId);


    /**
     * 根据套餐id查询套餐菜品
     * @param id
     * @return
     */
    List<SetmealDish> getDishBySetmealId(Long id);
}

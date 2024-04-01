package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入口味数据
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);


    /**
     * 删除口味
     * @param id
     */
    void deleteFlavorById(Long id);
    /**
     * 删除口味
     * @param ids
     */
    void deleteFlavorByIds(List<Long> ids);

    /**
     * 根据菜品id查询关联口味
     * @param dishId
     * @return
     */
    @Select("select * from dish_flavor where dish_id=#{dishId}")
    List<DishFlavor> getByDishId(Long dishId);
}

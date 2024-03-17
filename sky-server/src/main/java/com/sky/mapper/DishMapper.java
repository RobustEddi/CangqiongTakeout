package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}") // 这段 SQL 查询语句的作用是统计 dish 表中 category_id 列的值等于给定 categoryId 的记录数量。
    Integer countByCategoryId(Long categoryId);

}

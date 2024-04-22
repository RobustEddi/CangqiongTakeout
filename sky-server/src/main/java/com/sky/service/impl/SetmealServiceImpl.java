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
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        // 向套餐表插入1条数据
        setmealMapper.insert(setmeal);
        //TODO:20240416
        // 在套餐菜品表(setmeal_dish)里面写入套餐菜品信息
        // 遍历setmealDTO中的菜品arraylist
        // SetmealDTO中的属性setmealDishes列表中含有SetmealDish类，其中新建的每个setmealDish对象需要被赋值
        //赋值时，需要遍历setmealDishes列表，每次遍历的时候设之前需要设置一下setmealdish对象的属性
        SetmealDish setmealDish = new SetmealDish();
        setmealDish.setSetmealId(setmeal.getId());
        //遍历setmealDishes列表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish temp: setmealDishes
             ) {
            setmealDish.setDishId(temp.getDishId());
            setmealDish.setName(temp.getName());
            setmealDish.setPrice(temp.getPrice());
            setmealDish.setCopies(temp.getCopies());
            setmealDishMapper.insert(setmealDish);
        }
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        // 为了适应前端返回类型，泛型使用SetmealVO
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 套餐删除
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        // 判断当前套餐是否能够删除--是否存在起售中的套餐？
        for (Long id : ids
        ) {
            Setmeal setmeal = setmealMapper.getSetmealById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                //当前套餐处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //删除套餐表里面的套餐数据
        for (Long id : ids) {
          setmealMapper.deleteById(id);
      }

        //删除setmealDish表中的套餐关联菜品
        for (Long setmealId : ids) {
            setmealDishMapper.deleteBySetmealId(setmealId);
        }
    }

}

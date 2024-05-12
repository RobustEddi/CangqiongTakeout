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
import io.swagger.models.auth.In;
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

    @Autowired
    private DishMapper dishMapper;
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

    /**
     * 根据套餐id查询套餐
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        // 根据套餐id查询套餐
        Setmeal setmeal = setmealMapper.getSetmealById(id);
        // 根据套餐id查询菜品数据
        List<SetmealDish> setmealDishes = setmealDishMapper.getDishBySetmealId(id);
        // 将查询到的套餐、套餐菜品数据分装到SetmealVO
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    public void updateSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        // 修改套餐基本信息
        setmealMapper.update(setmeal);

        // 修改套餐菜品信息
        /// 删除原来套餐菜品
        setmealDishMapper.deleteBySetmealId(setmeal.getId());
        /// 添加新的套餐菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            //// 遍历为DTO中的套餐菜品属性添加setmealId信息
            for (SetmealDish setmealDish : setmealDishes
            ) {
                setmealDish.setSetmealId(setmeal.getId());
                setmealDishMapper.insert(setmealDish);
            }
        }
    }
    /**
     * 启停套餐
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // TODO 202404250045-->有个小bug
        Setmeal setmeal = setmealMapper.getSetmealById(id);
        //判断起售停售逻辑
        if (setmeal.getStatus() == 1 && status == 0) {
            /// 若原状态为起售，可将其设置为停售状态
            setmeal.setStatus(status);
            // 更新数据库
            setmealMapper.update(setmeal);

        } else{
            ///若原状态为停售，需判断套餐菜品是否包含停售菜品，不包含之后才能进行套餐起售
            //// 判断是否包含停售菜品（遍历套餐包含菜品）
            List<SetmealDish> setmealDishes = setmealDishMapper.getDishBySetmealId(id);
            for (SetmealDish setmealDish : setmealDishes
            ) {
                // 获取菜品状态
                Integer flag= dishMapper.getById(setmealDish.getDishId()).getStatus();
                ///菜品状态为停售，则不能起售套餐，需要报异常
                if (flag == 0) {
                    log.info("菜品状态为停售，不能起售套餐");
                    // 存在当前菜品被套餐关联情况，不能删除
                    throw new DeletionNotAllowedException("菜品状态为停售，不能起售套餐");
                }
            }
            /// 套餐不包含停售菜品，可以进行套餐起售
            setmeal.setStatus(status);
            // 更新数据库
            setmealMapper.update(setmeal);
        }
    }
}

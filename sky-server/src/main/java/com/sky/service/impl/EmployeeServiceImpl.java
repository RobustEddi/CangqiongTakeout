package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // TODO 后期需要进行md5加密，然后再进行比对
        // 对前端传过来的明文密码进行md5加密处理
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        //3、返回实体对象
        return employee;
    }


    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {

        System.out.println("当前线程id:"+Thread.currentThread().getId());

        //前端传入Service层的是DTO对象，service传入Dao持久层时，需要传入实体类对象
        Employee employee = new Employee();
//        employee.setName(employeeDTO.getName());

        //对象属性拷贝，直接将具备相同属性名的对象进行拷贝（前提是，属性名必须一致）
        BeanUtils.copyProperties(employeeDTO,employee);
        // 目标对象employee的属性更多一点，因此需要把目标对象中剩下的属性进行设置

        //设置密码，默认密码123456,存储到数据库的时候需要进行md5加密
        //默认密码存放于常量类PasswordConstant.DEFAULT_PASSWORD中
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //设置账号状态，利用common 文件夹下定义的常量类StatusConstant.ENABLE进行赋值
        employee.setStatus(StatusConstant.ENABLE);

        //设置当前记录的创建时间与修改时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        // 设置当前记录创建人id与修改人id
        // 后期需要改为当前登录用户的id(已修改)
//        employee.setCreateUser(10L);
//        employee.setUpdateUser(10L);
        // 将拦截器传过来的线程中的局部变量empId获取到
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());
        // 创建持久层employeeMapper中的insert方法，在持久层employeeMapper中插入封装好的employee对象
        employeeMapper.insert(employee);

    }
}




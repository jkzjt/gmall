package com.gmall.service;

import com.gmall.bean.UserAddress;
import com.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {

    /**
     * 校验用户状态
     *
     * @param userId
     * @return
     */
    UserInfo verify(String userId);

    /**
     * 用户登录
     *
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 根据userId查询用户地址列表
     *
     * @return
     */
    List<UserAddress> getUserAddressListByUserId(String userId);

    /**
     * 查询所有数据
     *
     * @return
     */
    List<UserInfo> listAllUserInfo();

}

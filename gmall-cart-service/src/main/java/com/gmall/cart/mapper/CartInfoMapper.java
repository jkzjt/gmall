package com.gmall.cart.mapper;

import com.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {

    List<CartInfo> selectCartListWithCurPrice(String userId);

}

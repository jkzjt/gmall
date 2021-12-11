package com.gmall.manage.mapper;

import com.gmall.bean.SkuInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuInfoMapper extends Mapper<SkuInfo> {

    SkuInfo selectByPK(String skuId);

    List<SkuInfo> selectBySpuId(String spuId);

}

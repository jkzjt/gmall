package com.gmall.manage.mapper;

import com.gmall.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {
    List<SpuSaleAttr> spuSaleAttrList(String spuId);

    List<SpuSaleAttr> selectBySkuIdAndSpuId(String skuId, String spuId);
}

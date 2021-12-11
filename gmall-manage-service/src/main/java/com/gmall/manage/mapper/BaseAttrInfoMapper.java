package com.gmall.manage.mapper;

import com.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    List<BaseAttrInfo> attrInfoList(String catalog3Id);

    List<BaseAttrInfo> selectByAttrValueIds(@Param("attrValueIds") String attrValueIds);
}

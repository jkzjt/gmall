package com.gmall.service;

import com.gmall.bean.SkuLsInfo;
import com.gmall.bean.SkuLsParams;
import com.gmall.bean.SkuLsResult;

public interface ListService {

    /**
     * 更新商品热度
     *      前台每访问一次商品详情，热度+1
     *
     * @param skuId
     */
    void incrHotScore(String skuId);

    /**
     * 根据skuLsParams（条件）查询 SkuLsResult
     *
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 保存skuLsInfo到ES中
     *
     * @param skuLsInfo
     */
    void saveSkuLsInfo2ES(SkuLsInfo skuLsInfo);

}

package com.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.SkuInfo;
import com.gmall.bean.SkuLsInfo;
import com.gmall.service.ListService;
import com.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("/onSale")
    public void onSale(String skuId){
        // 获取DB中的SKU信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        skuInfo.setSkuAttrValueList(manageService.getSkuAttrValueList(skuId));

        // 属性对拷到SkuLsInfo
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        BeanUtils.copyProperties(skuInfo, skuLsInfo);

        // 调用业务层保存到ES
        listService.saveSkuLsInfo2ES(skuLsInfo);
    }

    /**
     * http://localhost:8082/saveSkuInfo
     *
     * @param skuInfo
     */
    @RequestMapping("/saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        manageService.saveSkuInfo(skuInfo);
    }

}

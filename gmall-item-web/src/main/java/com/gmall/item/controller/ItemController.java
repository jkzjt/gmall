package com.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.SkuInfo;
import com.gmall.bean.SkuSaleAttrValue;
import com.gmall.bean.SpuSaleAttr;
import com.gmall.service.ListService;
import com.gmall.service.ManageService;
import com.gmall.web.annotation.LoginRequire;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;


    //@LoginRequire
    @RequestMapping("{skuId}.html")
    public String item(@PathVariable("skuId") String skuId, Model model) {

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        model.addAttribute("skuInfo", skuInfo);

        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(skuInfo);
        model.addAttribute("spuSaleAttrList", spuSaleAttrList);

        // 多个销售属性值确定一个唯一的sku
        List<SkuInfo> skuInfoList = manageService.getSkuInfoList(skuInfo.getSpuId());
        if (skuInfoList != null && skuInfoList.size() > 0) {
            Map<String, Object> map = new HashMap<>();
            for (SkuInfo info : skuInfoList) {
                String valuesSku = ""; // key 123
                String id = info.getId(); // value
                List<SkuSaleAttrValue> skuSaleAttrValueList = info.getSkuSaleAttrValueList();
                int len;
                if (skuSaleAttrValueList != null && (len = skuSaleAttrValueList.size()) > 0) {
                    for (int i = 0; i < len; i++) {
                        if ((i == 0 && len == 1) || i == len - 1) {
                            valuesSku += skuSaleAttrValueList.get(i).getSaleAttrValueId();
                        } else {
                            valuesSku += skuSaleAttrValueList.get(i).getSaleAttrValueId() + "|";
                        }
                    }
                    map.put(valuesSku, id);
                }
            }
            String jsonString = JSON.toJSONString(map);
            model.addAttribute("skuIds", jsonString);
        }

        listService.incrHotScore(skuId);

        return "item";

    }

}

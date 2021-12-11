package com.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.SpuImage;
import com.gmall.bean.SpuInfo;
import com.gmall.bean.SpuSaleAttr;
import com.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
public class SpuManageController {

    @Reference
    private ManageService manageService;

    /**
     * http://localhost:8082/spuImageList?spuId=58
     *
     * @param spuImage
     * @return
     */
    @RequestMapping("/spuImageList")
    public List<SpuImage> spuImageList(SpuImage spuImage) {
        return manageService.spuImageList(spuImage);
    }

    /**
     * http://localhost:8082/spuSaleAttrList?spuId=58
     *
     * @param spuId
     * @return
     */
    @RequestMapping("/spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {
        return manageService.spuSaleAttrList(spuId);
    }

    /**
     * http://localhost:8082/saveSpuInfo
     *
     * @param spuInfo
     */
    @RequestMapping("/saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        manageService.saveSpuInfo(spuInfo);
    }

    /**
     * http://localhost:8082/spuList?catalog3Id=61
     *
     * @param spuInfo
     * @return
     */
    @RequestMapping("/spuList")
    public List<SpuInfo> spuList(SpuInfo spuInfo) {
        return manageService.spuList(spuInfo);
    }

}

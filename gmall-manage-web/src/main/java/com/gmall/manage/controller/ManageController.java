package com.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.*;
import com.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
public class ManageController {

    @Reference
    private ManageService manageService;

    /**
     * http://localhost:8082/baseSaleAttrList
     *
     * @return
     */
    @RequestMapping("/baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList() {
        return manageService.baseSaleAttrList();
    }

    /**
     * http://localhost:8082/getAttrValueList?attrId=23
     *
     * @param attrId
     * @return
     */
    @RequestMapping("/getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        return manageService.getAttrValueList(attrId).getAttrValueList();
    }

    /**
     * http://localhost:8082/saveAttrInfo
     *
     * @param baseAttrInfo
     */
    @RequestMapping("/saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
    }

    /**
     * http://localhost:8082/attrInfoList?catalog3Id=69
     *
     * @param catalog3Id 三级分类ID
     * @return
     */
    @RequestMapping("/attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id) {
        List<BaseAttrInfo> baseAttrInfoList = manageService.attrInfoList(catalog3Id);
        return baseAttrInfoList;
    }

    /**
     * http://localhost:8082/getCatalog3?catalog2Id=15
     *
     * @param catalog2Id 二级分类ID
     * @return
     */
    @RequestMapping("/getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        return manageService.getCatalog3(catalog2Id);
    }

    /**
     * http://localhost:8082/getCatalog2?catalog1Id=2
     *
     * @param catalog1Id 一级分类ID
     * @return
     */
    @RequestMapping("/getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        return manageService.getCatalog2(catalog1Id);
    }

    /**
     * http://localhost:8082/getCatalog1
     *
     * @return
     */
    @RequestMapping("/getCatalog1")
    public List<BaseCatalog1> getCatalog1() {
        return manageService.getCatalog1();
    }

}

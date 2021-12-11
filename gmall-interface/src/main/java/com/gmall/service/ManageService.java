package com.gmall.service;

import com.gmall.bean.*;

import java.util.List;

public interface ManageService {

    /**
     * 根据多个平台属性值ID查询平台属性列表
     *
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(List<String> attrValueIdList);

    /**
     * 根据skuId查询SkuAttrValue(valueId)列表
     *
     * @param skuId
     * @return
     */
    List<SkuAttrValue> getSkuAttrValueList(String skuId);

    /**
     * 根据spuId查询SkuInfo列表
     *
     * @param spuId
     * @return
     */
    List<SkuInfo> getSkuInfoList(String spuId);

    /**
     * 根据skuInfo查询SpuSaleAttr列表
     *
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(SkuInfo skuInfo);

    /**
     * 根据skuId查询SkuInfo
     *
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 保存skuInfo
     *
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据spuId查询图片列表
     *
     * @param spuImage
     * @return
     */
    List<SpuImage> spuImageList(SpuImage spuImage);

    /**
     * 根据spuId查询销售属性列表
     *
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> spuSaleAttrList(String spuId);

    /**
     * 保存或修改spuInfo
     *
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 查询所有基本销售属性
     *
     * @return
     */
    List<BaseSaleAttr> baseSaleAttrList();

    /**
     * 根据spuInfo中的属性查询商品列表
     *
     * @param spuInfo
     * @return
     */
    List<SpuInfo> spuList(SpuInfo spuInfo);

    /**
     * 根据attrId查询属性值列表
     *
     * @param attrId 属性ID
     * @return
     */
    BaseAttrInfo getAttrValueList(String attrId);

    /**
     * 保存或修改属性和属性值
     *
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据catalog3Id查询属性列表
     *
     * @param catalog3Id 三级分类ID
     * @return
     */
    List<BaseAttrInfo> attrInfoList(String catalog3Id);

    /**
     * 根据catalog2Id查询三级分类列表
     *
     * @param catalog2Id 二级分类ID
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    /**
     * 根据catalog1Id查询二级分类列表
     *
     * @param catalog1Id 一级分类ID
     * @return
     */
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    /**
     * 查询所有一级分类列表
     *
     * @return
     */
    List<BaseCatalog1> getCatalog1();

}

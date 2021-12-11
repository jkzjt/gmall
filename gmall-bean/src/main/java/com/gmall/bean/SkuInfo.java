package com.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuInfo implements Serializable {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column
    private String spuId;

    @Column
    private BigDecimal price;

    @Column
    private String skuName;

    @Column
    private String skuDesc;

    @Column
    private BigDecimal weight;

    @Column
    private String tmId;

    @Column
    private String catalog3Id;

    @Column
    private String skuDefaultImg;

    @Transient
    private List<SkuAttrValue> skuAttrValueList; // 商品平台属性值集合

    @Transient
    private List<SkuSaleAttrValue> skuSaleAttrValueList; // 商品销售属性值集合

    @Transient
    private List<SkuImage> skuImageList; // 商品图片集合

}

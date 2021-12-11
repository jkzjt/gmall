package com.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CartInfo implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column
    private String id;

    @Column
    private String userId;

    @Column
    private String skuId;

    @Column
    private BigDecimal cartPrice; // 加入购物车时的价格

    @Column
    private Integer skuNum;

    @Column
    private String imgUrl;

    @Column
    private String skuName;

    // 实时价格 sku_info表中的价格
    @Transient
    private BigDecimal skuPrice;

    // 下订单的时候，商品是否勾选
    @Transient
    private String isChecked = "0";

}

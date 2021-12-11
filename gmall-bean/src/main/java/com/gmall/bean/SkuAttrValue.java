package com.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

@Data
public class SkuAttrValue implements Serializable {

    @Id
    @Column
    private String id;

    @Column
    private String attrId;

    @Column
    private String valueId;

    @Column
    private String skuId;

}

package com.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SkuLsResult implements Serializable {

    private List<SkuLsInfo> skuLsInfoList;

    private List<String> attrValueIdList;

    private long total;

    private long totalPages;
}

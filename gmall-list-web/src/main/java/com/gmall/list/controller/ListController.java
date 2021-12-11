package com.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.*;
import com.gmall.service.ListService;
import com.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    public String getList(SkuLsParams skuLsParams, Model model) {

        // skuLsParams.setPageSize(2);

        // skuLsResult 根据前台检索条件返回的结果
        SkuLsResult skuLsResult = listService.search(skuLsParams);

        // 商品集合
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();

        /*
            不管是全文检索还是分类检索都要显示罗列平台属性
                * 分类检索可以根据三级分类ID查询其下所有平台属性
                * 全文检索？？？
            ** 由于检索结果有平台属性值ID列表，可以根据其查询平台属性列表并罗列
         */
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();

        // 平台属性集合
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrInfoList(attrValueIdList);
        String[] valueIds = null;
        if ((valueIds = skuLsParams.getValueId()) != null && valueIds.length > 0) {
            // 面包屑集合
            List<BaseAttrValue> crumbs = new ArrayList<>();
            /*
                达到效果：
                    当用户点击平台属性值时，该平台属性移出列表,面包屑列表增加一组平台属性

                    TODO 由于数据结构设置的不够好，有待后期优化
             */
            // 将已选取的平台属性去除 itco
            for (String valueId : valueIds) {
                for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
                    BaseAttrInfo baseAttrInfo = iterator.next();
                    boolean flag = false; // 是否跳出本循环的标志
                    for (BaseAttrValue baseAttrValue : baseAttrInfo.getAttrValueList()) {
                        if (valueId.equals(baseAttrValue.getId())) {
                            // 制作面包屑
                            baseAttrValue.setValueName(baseAttrInfo.getAttrName() + ": <span style='color:red'>" + baseAttrValue.getValueName()
                                    + "</span>");
                            baseAttrValue.setQueryParams(makeQueryParams(skuLsParams, valueId));
                            crumbs.add(baseAttrValue);
                            // 移除
                            iterator.remove();
                            flag = true;
                            break; // 跳出本次循环
                        }
                    }
                    if (flag) {
                        break;
                    }
                }
            }

            model.addAttribute("crumbs", crumbs);
        }


        // 拼接查询条件
        String queryParams = makeQueryParams(skuLsParams);

        model.addAttribute("pageNo", skuLsParams.getPageNo());

        model.addAttribute("totalPages", skuLsResult.getTotalPages());

        model.addAttribute("keyword", skuLsParams.getKeyword());

        model.addAttribute("queryParams", queryParams);

        model.addAttribute("baseAttrInfoList", baseAttrInfoList);

        model.addAttribute("skuLsInfoList", skuLsInfoList);

        return "list";
    }

    /**
     * 根据前台检索条件制作地址栏url和面包屑url
     *
     * @param skuLsParams
     * @param excludedValueIds
     * @return
     */
    private String makeQueryParams(SkuLsParams skuLsParams, String... excludedValueIds) {

        StringBuilder stringBuilder = new StringBuilder("/list.html?");

        // 拼接keyword
        if (skuLsParams.getKeyword() != null) {
            stringBuilder.append("keyword=" + skuLsParams.getKeyword());
        }
        // 拼接三级分类ID
        if (skuLsParams.getCatalog3Id() != null) {
            stringBuilder.append("catalog3Id=" + skuLsParams.getCatalog3Id());
        }

        String[] valueIds = null;
        if ((valueIds = skuLsParams.getValueId()) != null && valueIds.length > 0) {
            // 拼接平台属性值ID
            for (String valueId : valueIds) {
                if (excludedValueIds != null && excludedValueIds.length > 0 && valueId.equals(excludedValueIds[0])) {
                    continue; // 拼接面包屑url
                } else {
                    stringBuilder.append("&valueId=" + valueId); // 拼接地址栏url
                }
            }
        }
        // 拼接页码
        // stringBuilder.append("&pageNo=" + skuLsParams.getPageNo());

        // System.out.println(stringBuilder);

        return stringBuilder.toString();
    }

}

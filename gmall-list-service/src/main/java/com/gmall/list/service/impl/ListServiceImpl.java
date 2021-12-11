package com.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.gmall.bean.SkuLsInfo;
import com.gmall.bean.SkuLsParams;
import com.gmall.bean.SkuLsResult;
import com.gmall.manage.util.RedisUtil;
import com.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    public static final String ES_INDEX = "gmall";

    public static final String ES_TYPE = "SkuInfo";

    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void incrHotScore(String skuId) {

        Jedis jedis = redisUtil.getJedis();
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
        if (hotScore % 10 == 0) {
            // 更新ES中商品的热度
            updateHotScore(skuId, Math.round(hotScore));
        }

    }

    private void updateHotScore(String skuId, Long hotScore) {
        /*
            * 更新
            1. 编写DSL语句
            2. 定义动作
            3. 执行动作
         */
        // 1. 编写DSL语句
        String updateStr = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\": " + hotScore + "\n" +
                "  }\n" +
                "}";
        // 2. 定义动作
        Update update = new Update.Builder(updateStr).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            // 3. 执行动作
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        /*
            * 商品检索 GET
            1. 定义DSL语句
                GET /gmall/SkuInfo/_search
                {
                    "query": {
                        "bool": {
                            "filter": [...],  // 过滤
                            "must": [...],  // 分词
                        }
                    },
                    "highlight": {...}, // 分词高亮
                    "aggs": {...},  // 聚合（分组）
                    "sort": [...],  // 排序
                    // 分页
                    "from": ...,
                    "size": ...
                }
            2. 定义动作
            3. 执行动作
            4. 获取结果集


         */
        // 1. 定义DSL语句
        String query = makeQueryStringForSearch(skuLsParams);

        // 2. 定义动作
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();

        SearchResult searchResult = null;
        try {
            // 3. 执行动作
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 4. 获取结果集
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);

        return skuLsResult;
    }

    /**
     * 根据前台的条件参数（skuLsParams）和ES返回的检索结果（searchResult）封装返回结果（SkuLsResult）
     *
     * @param skuLsParams
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();

        // 封装结果记录数 Total
        skuLsResult.setTotal(searchResult.getTotal());
        // 封装总页数
        long totalPages = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize(); // 总页数
        skuLsResult.setTotalPages(totalPages);

        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        if (hits != null && hits.size() > 0) {
            // 封装 skuLsResult.skuLsInfoList
            List<SkuLsInfo> skuLsInfoList = new ArrayList<>(skuLsParams.getPageSize());
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                // Map<String, List<String>> highlight = hit.highlight;
                if (hit.highlight != null && hit.highlight.size() > 0) {
                    // 将skuLsInfo中没有高亮标签的关键字替换
                    skuLsInfo.setSkuName(hit.highlight.get("skuName").get(0));
                }
                skuLsInfoList.add(skuLsInfo);
            }
            skuLsResult.setSkuLsInfoList(skuLsInfoList);
        }

        TermsAggregation groupby_attr = searchResult.getAggregations().getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets;
        if (groupby_attr != null && (buckets = groupby_attr.getBuckets()).size() > 0) {
            // 封装skuLsResult.attrValueIdList
            List<String> attrValueIdList = new ArrayList<>();
            for (TermsAggregation.Entry bucket : buckets) {
                attrValueIdList.add(bucket.getKey());
            }
            skuLsResult.setAttrValueIdList(attrValueIdList);
        }

        return skuLsResult;
    }

    /**
     * 根据skuLsParams中封装的条件组合检索的DSL语句
     *
     * @param skuLsParams
     * @return
     */
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 创建检索builder对象 {}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // "bool": {}
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        /*
            过滤
         */
        if (skuLsParams.getCatalog3Id() != null) {
            // 设置三级分类ID
            TermQueryBuilder catalog3Id = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(catalog3Id);
        }
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            // 设置平台属性值
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                TermQueryBuilder valueId = new TermQueryBuilder("skuAttrValueList.valueId", skuLsParams.getValueId()[i]);
                boolQueryBuilder.filter(valueId);
            }
        }

        /*
            检索
         */
        if (skuLsParams.getKeyword() != null) {
            // 设置关键字
            MatchQueryBuilder skuName = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(skuName);
            // 设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuName");
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");
            // 放入查询器
            searchSourceBuilder.highlight(highlightBuilder);
        }

        // 放入查询器
        searchSourceBuilder.query(boolQueryBuilder);

        // 设置聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId"));

        // 设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        // 设置分页
        int from = (skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize(); // 从第几条开始检索 0表示从第一条开始
        searchSourceBuilder.from(from).size(skuLsParams.getPageSize());

        // System.out.println(searchSourceBuilder.toString());

        return searchSourceBuilder.toString();
    }

    @Override
    public void saveSkuLsInfo2ES(SkuLsInfo skuLsInfo) {
        /*
            * 商品上架 PUT
            1. 定义动作 PUT /gmall/SkuInfo/id
            2. 执行动作
         */

        // 1. 定义动作 PUT /gmall/SkuInfo/id
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            // 2. 执行动作
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

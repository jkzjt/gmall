package com.gmall.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

    @Autowired
    private JestClient jestClient;

    @Test
    public void testES() {
        /*
            检索
                1. 定义dsl语句
                2. 定义动作
                3. 执行动作
                4. 获取结果集
         */

        // 1. 定义dsl语句
        String query = "{\n" +
                "  \"query\": {\n" +
                "    \"term\": {\n" +
                "      \"actorList.name\": \"张译\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // 2. 定义动作
        Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();


        try {
            // 3. 执行动作
            SearchResult searchResult = jestClient.execute(search);

            // 4. 获取结果集
            List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);
            for (SearchResult.Hit<Map, Void> hit : hits) {
                System.out.println(hit.source);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void contextLoads() {
    }

}

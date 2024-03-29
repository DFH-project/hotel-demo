package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class HotelSearchTest {

    private RestHighLevelClient client;


    @Test
    void testSearchAll() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchAllQuery());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        long value = hits.getTotalHits().value;  //总条数
        System.out.println("总共输出"+value+"条数据");
        hits.forEach(hit -> {
            System.out.println("hit===="+hit.getSourceAsString());
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            System.out.println("hotelDOC====" +hotelDoc);
        });
    }

    @Test
    void testSearchMatch() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        SearchRequest request2 = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchQuery("brand","如家"));
        request2.source().query(QueryBuilders.multiMatchQuery("如家","brand","all"));
        extracted(request);
        extracted(request2);
    }


    @Test
    void testSearch() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        boolQuery.must(QueryBuilders.termQuery("city","北京"));

        boolQuery.mustNot(QueryBuilders.termQuery("brand","如家"));

        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(500));

        request.source().query(boolQuery);
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false).preTags("<em>").postTags("</em>"));
        request.source().from(0).size(5);
        request.source().sort("price", SortOrder.DESC);

        extracted(request);
    }

    // ctrl_alt_M  代码抽取
    private void extracted(SearchRequest request) throws IOException {
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        long value = hits.getTotalHits().value;  //总条数
        System.out.println("总共输出"+value+"条数据");
        hits.forEach(hit -> {
            System.out.println("hit===="+hit.getSourceAsString());
            Map<String, HighlightField> fieldMap = hit.getHighlightFields();
            System.out.println(fieldMap.values());

            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            if (!fieldMap.isEmpty() || CollectionUtils.isEmpty(fieldMap)){
                Text[] names = fieldMap.get("name").getFragments();
                System.out.println("高亮展示结果==="+names[0].string());
                hotelDoc.setName(names[0].string());
            }
            System.out.println("hotelDOC====" +hotelDoc);
        });
    }

    @Test
    void testAggregation() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        request.source().size(0);
        request.source().aggregation(AggregationBuilders.terms("brandAgg").field("brand").size(10)).sort("price", SortOrder.DESC);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        Terms brandTerms =aggregations.get("brandAgg");
        brandTerms.getBuckets().forEach(bucket -> {
            System.out.println(bucket.getKeyAsString()+":"+bucket.getDocCount());
        });

    }


    @Test
    void testAgg2() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        Map<String, List<String>>  hashMap = new HashMap<>();
        request.source().size(0);
        request.source().aggregation(AggregationBuilders.terms("brandAgg").
                        field("brand").size(10)).
                sort("price", SortOrder.DESC);
        request.source().aggregation(AggregationBuilders.terms("cityAgg").
                        field("city").size(10)).
                sort("price", SortOrder.DESC);
        request.source().aggregation(AggregationBuilders.terms("starAgg").
                        field("starName").size(10)).
                sort("price", SortOrder.DESC);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        Terms brandTerms =aggregations.get("brandAgg");
        List<String> brandList = new ArrayList<>();
        brandTerms.getBuckets().forEach(bucket -> {
            System.out.println(bucket.getKeyAsString()+":"+bucket.getDocCount());
            brandList.add(bucket.getKeyAsString());
        });
        Terms cityTerms =aggregations.get("cityAgg");
        List<String> cityList = new ArrayList<>();
        cityTerms.getBuckets().forEach(bucket -> {
            System.out.println(bucket.getKeyAsString()+":"+bucket.getDocCount());
            cityList.add(bucket.getKeyAsString());
        });
        Terms starTerms =aggregations.get("starAgg");
        List<String> starList = new ArrayList<>();
        starTerms.getBuckets().forEach(bucket -> {
            System.out.println(bucket.getKeyAsString()+":"+bucket.getDocCount());
            starList.add(bucket.getKeyAsString());
        });

        hashMap.put("品牌",brandList);
        hashMap.put("城市",cityList);
        hashMap.put("星级",starList);

        System.out.println(hashMap);
    }

    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://39.96.116.202:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }



}

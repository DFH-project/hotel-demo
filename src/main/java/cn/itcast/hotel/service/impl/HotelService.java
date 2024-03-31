package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    @Autowired
    private RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams params) throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        String key = params.getKey();
        int size = params.getSize();
        int page = params.getPage();
        if (key==null || key.length()==0){
            request.source().query(QueryBuilders.matchAllQuery());
        }else{
            //1. 创建请求
            request.source().query(QueryBuilders.matchQuery("all",key));
        }

        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false).preTags("<em>").postTags("</em>"));
        request.source().from(page-1).size(size);
        request.source().sort("price", SortOrder.DESC);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        PageResult pageResult = this.extracted(response);
        return pageResult;
    }

    @Override
    public PageResult filters(RequestParams params) throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        buildQuery(params, request);


        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        PageResult pageResult = this.extracted(response);
        return pageResult;
    }

    private static void buildQuery(RequestParams params, SearchRequest request) {
        int size = params.getSize();
        int page = params.getPage();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (StringUtils.isNotEmpty(params.getKey())){
            request.source().query(QueryBuilders.matchQuery("all", params.getKey()));
        }else{
            request.source().query(QueryBuilders.matchAllQuery());
        }
        //条件过滤
        if (StringUtils.isNotEmpty(params.getCity())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        if (StringUtils.isNotEmpty(params.getBrand())){
            boolQueryBuilder.must(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        if (StringUtils.isNotEmpty(params.getStarName())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("starName", params.getStarName()));
        }
        if (StringUtils.isNotEmpty(params.getMinPrice()) && StringUtils.isNotEmpty(params.getMaxPrice())){
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(params.getMinPrice()).lte(params.getMaxPrice()));
        }


        // 算分控制
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(boolQueryBuilder,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                QueryBuilders.termQuery("adFlag",true),
                                ScoreFunctionBuilders.weightFactorFunction(10)
                        )
                });

        request.source().query(functionScoreQueryBuilder);

        //request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false).preTags("<em>").postTags("</em>"));
        request.source().from(page-1).size(size);
        request.source().sort("price", SortOrder.DESC);
        if (StringUtils.isNotEmpty(params.getLocation())){
            request.source().sort(SortBuilders.geoDistanceSort("location",
                    new GeoPoint(params.getLocation())).order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS));
        }
    }

    @Override
    public Map<String, List<String>> getFilters(RequestParams params) throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        Map<String, List<String>>  hashMap = new HashMap<>();
        buildQuery(params, request);
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
        starTerms.getBuckets().forEach(bucket -> {
            System.out.println(bucket.getKeyAsString()+":"+bucket.getDocCount());
            cityList.add(bucket.getKeyAsString());
        });

        hashMap.put("品牌",brandList);
        hashMap.put("城市",cityList);
        hashMap.put("星级",cityList);

        return hashMap;
    }

    @Override
    public List<String> getSuggestions(String prefix) throws IOException {
        SearchRequest request=new SearchRequest("hotel");
        request.source().suggest(new SuggestBuilder().addSuggestion(
                        "suggestTest", SuggestBuilders.completionSuggestion("suggestion").prefix(prefix)
                                .skipDuplicates(true).size(10)
                ));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        Suggest suggest = response.getSuggest();

        CompletionSuggestion suggestion = suggest.getSuggestion("suggestTest");

        List<CompletionSuggestion.Entry.Option> optionList = suggestion.getOptions();
        List<String> list = new ArrayList<>();
        optionList.forEach(option ->{
            String text = option.getText().toString();
            list.add(text);
        });
        return list;
    }

    @Override
    public void insertById(Long id) {
        try {
            Hotel hotel=getById(id);

            HotelDoc hotelDoc=new HotelDoc(hotel);

            IndexRequest request=new IndexRequest("hotel").id(hotel.getId().toString());
            // 准备JSON文档
            request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);

            client.index(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delById(Long id) {
        try {
            DeleteRequest deleteRequest=new DeleteRequest("hotel", String.valueOf(id));


            client.delete(deleteRequest,  RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private PageResult extracted( SearchResponse response) throws IOException {
        PageResult pageResult = new PageResult();
        List<HotelDoc> hotels = new ArrayList<>();
        SearchHits hits = response.getHits();
        long value = hits.getTotalHits().value;  //总条数
        pageResult.setTotal(value);
        System.out.println("总共输出"+value+"条数据");
        hits.forEach(hit -> {
            System.out.println("hit===="+hit.getSourceAsString());
            Map<String, HighlightField> fieldMap = hit.getHighlightFields();

            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            if (! CollectionUtils.isEmpty(fieldMap)){
                Text[] names = fieldMap.get("name").getFragments();
                System.out.println("高亮展示结果==="+names[0].string());
                hotelDoc.setName(names[0].string());
            }

            Object[] location=hit.getSortValues();
            if (location.length>0){
                Object o = location[0];
                hotelDoc.setDistance(o);
            }
           // System.out.println("hotelDOC====" +hotelDoc);
            hotels.add(hotelDoc);
        });
        pageResult.setHotels(hotels);
        return pageResult;
    }
}

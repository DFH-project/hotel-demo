package cn.itcast.hotel.pojo;

import lombok.Data;

@Data
public class RequestParams {

    private String key;

    private Integer page;
    private Integer size;

    private String city;

    private String starName;


    private String brand;

    private String maxPrice;

    private String minPrice;
    private String sortBy;

    private String location;
}

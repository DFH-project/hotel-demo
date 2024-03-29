package cn.itcast.hotel.pojo;

import lombok.Data;

import java.util.List;

@Data
public class PageResult {

    public Long total;

    public List<HotelDoc> hotels;
}

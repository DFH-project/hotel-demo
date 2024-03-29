package cn.itcast.hotel.controller;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {

    @Autowired
    private IHotelService hotelService;
    @PostMapping(value = "/list")
    public PageResult search(@RequestBody RequestParams params) throws IOException {
        PageResult pageResult = hotelService.search(params);
        return pageResult;
    }

    @PostMapping(value = "/filters")
    public PageResult filters(@RequestBody RequestParams params) throws IOException {
        PageResult pageResult = hotelService.filters(params);
        return pageResult;
    }

    @PostMapping(value = "/filters2")
    public  Map<String, List<String>> filters2() throws IOException {
        Map<String, List<String>> map = hotelService.getFilters();
        return map;
    }

}

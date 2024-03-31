package cn.itcast.hotel.controller;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {

    @Autowired
    private IHotelService hotelService;
    @PostMapping(value = "/list2")
    public PageResult search(@RequestBody RequestParams params) throws IOException {
        PageResult pageResult = hotelService.search(params);
        return pageResult;
    }

    @PostMapping(value = "/list")
    public PageResult filters(@RequestBody RequestParams params) throws IOException {
        PageResult pageResult = hotelService.filters(params);
        return pageResult;
    }

    @PostMapping(value = "/filters")
    public  Map<String, List<String>> filters2(@RequestBody RequestParams params) throws IOException {
        Map<String, List<String>> map = hotelService.getFilters(params);
        return map;
    }

    @GetMapping("suggestion")
    public List<String> getSuggestions(@RequestParam("key") String preFix) throws IOException {
        List<String> list = hotelService.getSuggestions(preFix);
        return list;
    }

}

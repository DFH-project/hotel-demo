package cn.itcast.hotel.constants;

public class MQConstants {

    //交换机
    public  final  static String HOTEL_EXCHANGE= "hotel.exchange";

    // 队列 修改/新增
    public  final  static String HOTEL_INSERT_QUEUE= "hotel.insert.queue";

    // 队列 删除
    public  final  static String HOTEL_DELETE_QUEUE= "hotel.delete.queue";

    // 新增/修改 routting key
    public  final  static String HOTEL_INSERT_KEY= "hotel.insert";

    // 队列 删除
    public  final  static String HOTEL_DELETE_KEY= "hotel.delete";

}

package cn.itcast.hotel.mq;

import cn.itcast.hotel.constants.MQConstants;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class hotelListener {

    @Autowired
    private IHotelService hotelService;

    @RabbitListener(queues = MQConstants.HOTEL_INSERT_QUEUE)
    public void listenHotelInsertOrUpdate(Long id){
        hotelService.insertById(id);
    }


    @RabbitListener(queues = MQConstants.HOTEL_DELETE_QUEUE)
    public void listenHotelDelete(Long id) throws IOException {
        hotelService.delById(id);
    }

}

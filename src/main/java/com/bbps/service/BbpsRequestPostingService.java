package com.bbps.service;


import com.bbps.constants.KafkaTopics;
import com.bbps.kafka.KafkaMessagePostingService;
import com.bbps.kafka.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class BbpsRequestPostingService {


    KafkaMessagePostingService kafkaMessagePostingService;
    public void post(Message message) throws JsonProcessingException {

        Map<String,Object> postMessageMap=new HashMap();
        postMessageMap.put("Key","key");
        postMessageMap.put("test","test");
        kafkaMessagePostingService.postMessage(postMessageMap, KafkaTopics.BBPS_POSTING_REQUEST);


    }
}

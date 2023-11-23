package com.bbps.kafka;

import com.bbps.kafka.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.Map;


@Component
public class KafkaResponseMessage {
    private KafkaResponseMessage(){

    }

    public static Message constructMessage(Map<String, Object> obj){
        Message message = new Message();

        Message.BbpsReqinfo appInfo = new Message.BbpsReqinfo();

        Map<String,Object> headerMap = new HashMap<>();
        appInfo.setHeaders(headerMap);

        Message.MessageBody messageBody = new Message.MessageBody();
        messageBody.setBody(obj.toString());
        appInfo.setMessageBody(messageBody);

        message.setBbpsReqinfo(appInfo);
        return message;
    }

}
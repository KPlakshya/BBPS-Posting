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

        Message.BbpsReqInfo appInfo = new Message.BbpsReqInfo();

        Map<String,Object> headerMap = new HashMap<>();
       // headerMap.put("tenant", TenantContext.getTenantId());
        //headerMap.put("userid",TenantContext.getUserId());
        appInfo.setHeaders(headerMap);

        Message.MessageBody messageBody = new Message.MessageBody();
        messageBody.setBody(obj.toString());
        appInfo.setMessageBody(messageBody);

        message.setBbpsReqInfo(appInfo);
        return message;
    }

    public static String decodeMessage(String reqMsg) throws JsonProcessingException {
        Message message = new ObjectMapper().readValue(reqMsg, Message.class);
      //  TenantContext.setTenantId(message.getAppInfo().getHeaders().get(FundPostConstants.TENANT).toString());
       // TenantContext.setUserId(message.getAppInfo().getHeaders().get(FundPostConstants.TENANT_USER_ID).toString());
        return new ObjectMapper().writeValueAsString(message.getBbpsReqInfo().getMessageBody().getBody());
    }





}
package com.bbps.kafka.model;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
public class Message {

    private BbpsReqInfo bbpsReqInfo;

    @Data
    @ToString
    public static class BbpsReqInfo {
        private Map<String, Object> headers;
        private MessageBody messageBody;
    }

    @Data
    @ToString
    public static class MessageBody {
        private String body;
    }

}
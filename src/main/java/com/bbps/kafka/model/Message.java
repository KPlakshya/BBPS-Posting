package com.bbps.kafka.model;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
public class Message {

    private BbpsReqinfo bbpsReqinfo;

    @Data
    @ToString
    public static class BbpsReqinfo {
        private Map<String, Object> headers;
        private MessageBody messageBody;
    }

    @Data
    @ToString
    public static class MessageBody {
        private String body;
    }

}
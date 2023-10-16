package com.bbps.kafka;

import com.bbps.kafka.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class KafkaMessagePostingService {

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    KafkaResponseMessage kafkaResponseMessage;

    public void postMessage(Map<String, Object> requestMsgMap, String queueName) throws JsonProcessingException {
        Message message =KafkaResponseMessage.constructMessage(requestMsgMap);
        log.info("Posting Request -QueueName[{}] , Message [{}]",queueName, message);
        kafkaTemplate.send(queueName, new ObjectMapper().writeValueAsString(message));
        log.info("Request Posted  SuccessFully -QueueName[{}]",queueName);

    }
}
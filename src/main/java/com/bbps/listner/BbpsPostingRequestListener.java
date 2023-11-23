package com.bbps.listner;

import com.bbps.kafka.model.Message;
import com.bbps.service.PostingProcessServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import static com.bbps.constants.KafkaTopics.BBPS_POSTING_REQUEST;
import static com.bbps.constants.KafkaTopics.BBPS_POSTING_REQUEST_GROUP;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BbpsPostingRequestListener {

    @Autowired
    PostingProcessServiceImpl postingProcessService;


    @KafkaListener(topics =BBPS_POSTING_REQUEST,groupId = BBPS_POSTING_REQUEST_GROUP)
    public void getPostingRequest(@Payload String kafkaReqData, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic){
        try {
            log.info("Topic [{}] ,message received From CustomerApp for Posting [{}]", topic, kafkaReqData);
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                Message message = mapper.readValue(kafkaReqData,Message.class);
                postingProcessService.process(message);
        } catch (JsonProcessingException jpe) {
           log.error("Topic [{}] ,JsonProcessing Exception While Receiving Request [{}] ,Exception [{}]", topic, kafkaReqData, jpe);
        } catch (Exception e) {
            log.error("Topic [{}] ,Exception While Receiving Request [{}] ,Exception [{}]", topic, kafkaReqData, e);
        }
    }
}

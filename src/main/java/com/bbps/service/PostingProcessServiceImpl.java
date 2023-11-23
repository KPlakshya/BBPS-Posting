package com.bbps.service;

import org.springframework.stereotype.Service;

import com.bbps.constants.Constants;
import com.bbps.kafka.model.Message;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class PostingProcessServiceImpl {

    public void process(Message message){
        try {
			GetProcess.getServiceImpl((String)message.getBbpsReqinfo().getHeaders().get(Constants.ReqType)).process(message);
		} catch (Exception e) {
			log.error("unable to proceed with the bbps request {}", e.getMessage());
		}
        

    }
}

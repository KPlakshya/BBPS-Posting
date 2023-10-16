package com.bbps.controller;

import com.bbps.kafka.model.Message;
import com.bbps.service.BbpsRequestPostingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value="/bbps/npci/")
public class TestRequestController {

    @Autowired
    BbpsRequestPostingService npciRequestPostingService;

    @PostMapping(value = "/test",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Message> getRequest(Message message) throws JsonProcessingException {
        log.info("Request Recived from controller [{}]",message);
        npciRequestPostingService.post(message);
        return ResponseEntity.status(HttpStatus.OK).body(message);

    }
}

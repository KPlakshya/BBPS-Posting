package com.bbps.service;

import com.bbps.kafka.model.Message;

public interface BBPSService {

	public void process(Message message);

}

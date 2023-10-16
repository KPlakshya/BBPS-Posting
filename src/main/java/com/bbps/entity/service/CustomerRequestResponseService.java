package com.bbps.entity.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bbps.billerfetch.data.BillerFetchResponse;
import com.bbps.constants.Constants;
import com.bbps.entity.CustomerRequestResponse;
import com.bbps.entity.repo.CustomerRequestResponseRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CustomerRequestResponseService {
	
	@Autowired
	private CustomerRequestResponseRepo repo;

	public void fetchAndUpdateIntiated(String id, String httpcode, String refId) {
		Optional<CustomerRequestResponse> custreqresp = repo.findById(Long.valueOf(id));
		if (custreqresp.isPresent()) {
			CustomerRequestResponse reqresp = custreqresp.get();
			reqresp.setRefId(refId);
			reqresp.setRequestHttpStatus(httpcode);
			reqresp.setStatus(Constants.INTIATED);
			log.info("changing the status to initiated for customer_req_id {}", id);
			repo.save(reqresp);
		} else {
			log.error("No request found for the customer_req_id in Intiated {}", id);
		}
	}

	public void fetchAndUpdateFailure(String id, String httpcode, String refId, Object response) {
		Optional<CustomerRequestResponse> custreqresp = repo.findById(Long.valueOf(id));
		if (custreqresp.isPresent()) {
			CustomerRequestResponse reqresp = custreqresp.get();
			reqresp.setRefId(refId);
			reqresp.setRequestHttpStatus(httpcode);
			reqresp.setResponse(serializeToJson(response, id));
			reqresp.setResponseTimestamp(Timestamp.valueOf(LocalDateTime.now()));
			reqresp.setStatus(Constants.FAILURE);
			log.info("changing the status to failure for customer_req_id {}", id);
			repo.save(reqresp);
		} else {
			log.error("No request found for the customer_req_id in Failure {}", id);
		}

	}
	
	 public static <T> String serializeToJson(T object, String id)  {
		 try {
			 ObjectMapper objectMapper = new ObjectMapper();
			 return objectMapper.writeValueAsString(object);
		 }catch (Exception e) {
			 log.error("unable to convert failure response to string {}", id);
			return null;
		}
	        
	    }

}

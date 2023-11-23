package com.bbps.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bbps.constants.Constants;
import com.bbps.service.impl.BbpsAgentFetchServiceImpl;
import com.bbps.service.impl.BbpsBillFetchServiceImpl;
import com.bbps.service.impl.BbpsBillPaymentServiceImpl;
import com.bbps.service.impl.BbpsBillValServiceImpl;
import com.bbps.service.impl.BbpsBillerFetchServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GetProcess {

	@Autowired
	public BbpsBillFetchServiceImpl bbpsBillFetchServiceImpl;

	@Autowired
	public BbpsBillerFetchServiceImpl bbpsBillerFetchServiceImpl;

	@Autowired
	public BbpsBillValServiceImpl bbpsBillValidationServiceImpl;

	@Autowired
	public BbpsBillPaymentServiceImpl bbpsBillPaymentServiceImpl;

	@Autowired
	public BbpsAgentFetchServiceImpl bbpsAgentFetchServiceImpl;

	public static final Map<String, BBPSService> svsImpl = new HashMap<String, BBPSService>();

	@PostConstruct
	public void init() {
		log.info("loading service class");
		svsImpl.put(Constants.BILL_FETCH_REQUEST, bbpsBillFetchServiceImpl);
		svsImpl.put(Constants.BILLER_FETCH_REQUEST, bbpsBillerFetchServiceImpl);
		svsImpl.put(Constants.BILL_VALIDATION_REQUEST, bbpsBillValidationServiceImpl);
		svsImpl.put(Constants.BILL_PAYMENT_REQUEST, bbpsBillPaymentServiceImpl);
		svsImpl.put(Constants.AGENT_FETCH_REQUEST, bbpsAgentFetchServiceImpl);

	}

	public static BBPSService getServiceImpl(String requestType) throws Exception {
		log.info("Fetching service class for request type [{}]", requestType);
		return Optional.ofNullable(svsImpl.get(requestType))
				.orElseThrow(() -> new IllegalArgumentException("bbps request type is incorrect"));

	}
}

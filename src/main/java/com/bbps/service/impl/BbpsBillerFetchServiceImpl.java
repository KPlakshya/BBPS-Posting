package com.bbps.service.impl;

import org.bbps.schema.BillerFetchRequestType;
import org.bbps.schema.MyBiller;
import org.bbps.schema.SearchByTime;
import org.bbps.schema.SearchMyBiller;
import org.bbps.schema.SearchType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bbps.billerfetch.data.BillerFetchRequest;
import com.bbps.billerfetch.data.BillerFetchResponse;
import com.bbps.constants.Constants;
import com.bbps.data.BbpsPostingResponse;
import com.bbps.entity.service.CustomerRequestResponseService;
import com.bbps.kafka.model.Message;
import com.bbps.service.BBPSService;
import com.bbps.service.BbpsRestConService;
import com.bbps.service.Utils;
import com.bbps.utils.MarshUnMarshUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service("BillerFetchRequest")
@Slf4j
public class BbpsBillerFetchServiceImpl implements BBPSService {

	@Value("$bbps.orgInst")
	private String orgId;

	@Value("$bbps.prefix")
	private String prefix;

	@Autowired
	private BbpsRestConService bbpsRestConService;

	@Autowired
	private CustomerRequestResponseService custReqRespService;

	@Override
	public void process(Message message) {
		log.info("inside BbpsBillerFetchServiceImpl process [{}]", message.toString());
		String billerFetchStr = null;
		BbpsPostingResponse bbpspostingresp = null;
		BillerFetchRequestType billerFetch = null;
		try {

			billerFetch = getBillerFetchRequestXML(message.getBbpsReqInfo().getMessageBody().getBody());
			billerFetchStr = MarshUnMarshUtil.marshal(billerFetch).toString();
			bbpspostingresp = bbpsRestConService.send(billerFetchStr, Constants.BILLER_FETCH_REQUEST,
					billerFetch.getHead().getRefId());

		} catch (Exception e) {

			log.error("Unable to process biller fetch request [{}]", e.getMessage());
			bbpspostingresp = new BbpsPostingResponse();
			bbpspostingresp.setErrorCode(Constants.ERROR_CODE_99);
			bbpspostingresp.setAck(Constants.ERROR_MSG_99);

		} finally {
			String id = message.getBbpsReqInfo().getHeaders().get(Constants.CUSTOMER_REQ_ID).toString();
			String refId = billerFetch != null ? billerFetch.getHead().getRefId() : null;
			if (bbpspostingresp.getAckerror() != null) {

				BillerFetchResponse response = new BillerFetchResponse();
				response.setResponseCode(bbpspostingresp.getErrorCode());
				response.setResponseMessage(bbpspostingresp.getAckerror());
				custReqRespService.fetchAndUpdateFailure(id, bbpspostingresp.getHttpcode(), refId, response);
			} else {
				custReqRespService.fetchAndUpdateIntiated(id, bbpspostingresp.getHttpcode(), refId);
			}

			// save in audit table
		}

	}

	private BillerFetchRequestType getBillerFetchRequestXML(String body)
			throws JsonMappingException, JsonProcessingException {
		BillerFetchRequest request = getRequest(body);
		BillerFetchRequestType xmlrequest = new BillerFetchRequestType();
		xmlrequest.setHead(Utils.createHead(orgId, prefix));
		SearchMyBiller searchMyBiller = new SearchMyBiller();
		searchMyBiller.setMybiller(MyBiller.valueOf(request.getSearchMyBiller()));
		xmlrequest.setSearchMyBiller(searchMyBiller);
		SearchType searchType = new SearchType();
		searchType.getBillerId().add(request.getBillerId());
		searchType.getBillerCategoryName().add(request.getBillerCategoryName());
		SearchByTime searchByTime = new SearchByTime();
		searchByTime.setTime(Utils.generateTs());
		xmlrequest.setSearch(searchType);
		xmlrequest.setSearchByTime(searchByTime);
		return xmlrequest;

	}

	public static BillerFetchRequest getRequest(String reqStr) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		BillerFetchRequest reqJson = mapper.readValue(reqStr, BillerFetchRequest.class);

		return reqJson;
	}

}

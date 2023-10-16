package com.bbps.service.impl;

import org.bbps.schema.AgentFetchRequestType;
import org.bbps.schema.SearchByTime;
import org.bbps.schema.SearchTypeForAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bbps.agentfetch.data.AgentFetchRequest;
import com.bbps.agentfetch.data.AgentFetchResponse;
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

@Service("AgentFetchRequest")
@Slf4j
public class BbpsAgentFetchServiceImpl implements BBPSService {

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
		log.info("inside BbpsAgentFetchServiceImpl process [{}]", message.toString());
		String agentFetchStr = null;
		BbpsPostingResponse bbpspostingresp = null;
		AgentFetchRequestType agentfetch = null;
		try {

			agentfetch = getAgentFetchRequestXML(message.getBbpsReqInfo().getMessageBody().getBody());
			agentFetchStr = MarshUnMarshUtil.marshal(agentfetch).toString();
			bbpspostingresp = bbpsRestConService.send(agentFetchStr, Constants.AGENT_FETCH_REQUEST,
					agentfetch.getHead().getRefId());

		} catch (Exception e) {

			log.error("Unable to process agent fetch request [{}]", e.getMessage());
			bbpspostingresp = new BbpsPostingResponse();
			bbpspostingresp.setErrorCode(Constants.ERROR_CODE_99);
			bbpspostingresp.setAck(Constants.ERROR_MSG_99);

		} finally {
			String id = message.getBbpsReqInfo().getHeaders().get(Constants.CUSTOMER_REQ_ID).toString();
			String refId = agentfetch != null ? agentfetch.getHead().getRefId() : null;
			if (bbpspostingresp.getAckerror() != null) {

				AgentFetchResponse response = new AgentFetchResponse();
				response.setResponseCode(bbpspostingresp.getErrorCode());
				response.setResponseMessage(bbpspostingresp.getAckerror());
				custReqRespService.fetchAndUpdateFailure(id, bbpspostingresp.getHttpcode(), refId, response);
			} else {
				custReqRespService.fetchAndUpdateIntiated(id, bbpspostingresp.getHttpcode(), refId);
			}

			// save in audit table
		}

	}

	private AgentFetchRequestType getAgentFetchRequestXML(String body)
			throws JsonMappingException, JsonProcessingException {
		AgentFetchRequest request = getRequest(body);
		AgentFetchRequestType xmlrequest = new AgentFetchRequestType();
		xmlrequest.setHead(Utils.createHead(orgId, prefix));
		SearchTypeForAgent searchType = new SearchTypeForAgent();
		searchType.getAgentId().add(request.getAgentId());
		SearchByTime searchByTime = new SearchByTime();
		searchByTime.setTime(Utils.generateTs());
		xmlrequest.setSearch(searchType);
		xmlrequest.setSearchByTime(searchByTime);
		return xmlrequest;

	}

	public static AgentFetchRequest getRequest(String reqStr) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		AgentFetchRequest reqJson = mapper.readValue(reqStr, AgentFetchRequest.class);

		return reqJson;
	}
}

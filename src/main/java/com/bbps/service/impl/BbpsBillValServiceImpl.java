package com.bbps.service.impl;

import org.bbps.schema.AgentType;
import org.bbps.schema.BillDetailsType;
import org.bbps.schema.BillValidationRequestType;
import org.bbps.schema.BillerType;
import org.bbps.schema.CustomerParamsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bbps.billvalidation.data.BillValidationRequest;
import com.bbps.billvalidation.data.BillValidationResponse;
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

@Service("BillValidationRequest")
@Slf4j
public class BbpsBillValServiceImpl implements BBPSService{
	
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
		log.info("inside BbpsBillValServiceImpl process [{}]", message.toString());
		String billvalReqStr = null;
		BbpsPostingResponse bbpspostingresp = null;
		BillValidationRequestType billvalReq = null;
		try {

			billvalReq = getBillValRequestXML(message.getBbpsReqInfo().getMessageBody().getBody());
			billvalReqStr = MarshUnMarshUtil.marshal(billvalReq).toString();
			bbpspostingresp = bbpsRestConService.send(billvalReqStr, Constants.BILL_VALIDATION_REQUEST,
					billvalReq.getHead().getRefId());

		} catch (Exception e) {

			log.error("Unable to process bill validation request [{}]", e.getMessage());
			bbpspostingresp = new BbpsPostingResponse();
			bbpspostingresp.setErrorCode(Constants.ERROR_CODE_99);
			bbpspostingresp.setAck(Constants.ERROR_MSG_99);

		} finally {
			String id = message.getBbpsReqInfo().getHeaders().get(Constants.CUSTOMER_REQ_ID).toString();
			String refId = billvalReq != null ? billvalReq.getHead().getRefId() : null;
			if (bbpspostingresp.getAckerror() != null) {

				BillValidationResponse response = new BillValidationResponse();
				response.setResponseCode(bbpspostingresp.getErrorCode());
				response.setResponseMessage(bbpspostingresp.getAckerror());
				custReqRespService.fetchAndUpdateFailure(id, bbpspostingresp.getHttpcode(), refId, response);
			} else {
				custReqRespService.fetchAndUpdateIntiated(id, bbpspostingresp.getHttpcode(), refId);
			}

			// save in audit table
		}

	}

	private BillValidationRequestType getBillValRequestXML(String body) throws JsonMappingException, JsonProcessingException {
		BillValidationRequest request = getRequest(body);
		BillValidationRequestType xmlReq = new BillValidationRequestType();
		xmlReq.setHead(Utils.createHead(orgId, prefix));
		AgentType agentType = new AgentType();
		agentType.setId(request.getAgentId());
		xmlReq.setAgent(agentType);
		BillDetailsType billDtl = new BillDetailsType();
		BillerType billtype = new BillerType();
		billtype.setId(request.getBillerId());
		billDtl.setBiller(billtype);
		CustomerParamsType custparam = new CustomerParamsType();
		for (int i = 0; i < request.getInputParams().getInput().size(); i++) {
			CustomerParamsType.Tag ct = new CustomerParamsType.Tag();
			ct.setName(request.getInputParams().getInput().get(i).getParamName());
			ct.setValue(request.getInputParams().getInput().get(i).getParamValue());
			custparam.getTag().add(ct);
		}
		billDtl.setCustomerParams(custparam);
		xmlReq.setBillDetails(billDtl);
		
		return xmlReq;
		
		
		
	}
	
	public static BillValidationRequest getRequest(String reqStr) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		BillValidationRequest reqJson = mapper.readValue(reqStr, BillValidationRequest.class);

		return reqJson;
	}


}

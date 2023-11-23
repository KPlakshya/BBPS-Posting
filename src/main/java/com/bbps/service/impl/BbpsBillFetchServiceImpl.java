package com.bbps.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.bbps.schema.AgentType;
import org.bbps.schema.AnalyticsFetchTypeInstance;
import org.bbps.schema.AnalyticsType;
import org.bbps.schema.BillDetailsType;
import org.bbps.schema.BillFetchRequest;
import org.bbps.schema.BillerType;
import org.bbps.schema.CustomerDtlsType;
import org.bbps.schema.CustomerParamsType;
import org.bbps.schema.DeviceTagNameType;
import org.bbps.schema.DeviceType;
import org.bbps.schema.DirectBillChannelType;
import org.bbps.schema.RiskScoresType;
import org.bbps.schema.TxnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bbps.billfetch.data.BillFetchRequestVO;
import com.bbps.billfetch.data.BillFetchResponseVO;
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

@Service("BillFetchRequest")
@Slf4j
public class BbpsBillFetchServiceImpl implements BBPSService {
	@Value("${bbps.orgInst}")
	private String orgId;

	@Value("${bbps.prefix}")
	private String prefix;

	@Autowired
	private BbpsRestConService bbpsRestConService;

	@Autowired
	private CustomerRequestResponseService custReqRespService;

	@Override
	public void process(Message message) {
		log.info("bbps-BillFetch Process Request [{}]", message.toString());
		String billFetchStr = null;
		BbpsPostingResponse bbpspostingresp = null;
		BillFetchRequest billFetch = null;

		try {
			billFetch = getBBPSXmlRequest(message.getBbpsReqinfo().getMessageBody().getBody());
			billFetchStr = MarshUnMarshUtil.marshal(billFetch).toString();

			bbpspostingresp = bbpsRestConService.send(billFetchStr, Constants.BILL_FETCH_REQUEST,
					billFetch.getHead().getRefId());
			log.info("bbpspostingresp String[{}]",bbpspostingresp);
		} catch (Exception e) {
			log.error("Unable to process Bill fetch request [{}]", e.getMessage());
			bbpspostingresp = new BbpsPostingResponse();
			bbpspostingresp.setErrorCode(Constants.ERROR_CODE_99);
			bbpspostingresp.setAck(Constants.ERROR_MSG_99);

		} finally {
			String id = message.getBbpsReqinfo().getHeaders().get(Constants.CUSTOMER_REQ_ID).toString();
			String refId = billFetch != null ? billFetch.getHead().getRefId() : null;
			if (bbpspostingresp.getAckerror() != null) {
				BillFetchResponseVO response = new BillFetchResponseVO();
				response.setResponseCode(bbpspostingresp.getErrorCode());
				response.setResponseMessage(bbpspostingresp.getAckerror());
				custReqRespService.fetchAndUpdateFailure(id, bbpspostingresp.getHttpcode(), refId, response);
			} else {
				custReqRespService.fetchAndUpdateIntiated(id, bbpspostingresp.getHttpcode(), refId);
			}

			// save in audit table
		}

	}

	private BillFetchRequest getBBPSXmlRequest(String body) throws JsonMappingException, JsonProcessingException {
		BillFetchRequestVO request = getRequest(body);
		BillFetchRequest billFetchRequestType = new BillFetchRequest();
		billFetchRequestType.setHead(Utils.createHead(orgId, prefix));
		AnalyticsType analyticsType = new AnalyticsType();
		AnalyticsType.Tag t1 = new AnalyticsType.Tag();
		t1.setName(AnalyticsFetchTypeInstance.FETCHREQUESTSTART.value());
		t1.setValue(Utils.generateTs());
		AnalyticsType.Tag t2 = new AnalyticsType.Tag();
		t2.setName(AnalyticsFetchTypeInstance.FETCHREQUESTEND.value());
		t2.setValue(Utils.generateTs());
		analyticsType.getTags().add(t1);
		analyticsType.getTags().add(t2);
		billFetchRequestType.setAnalytics(analyticsType);
		TxnType txn = new TxnType();
		txn.setTs(Utils.generateTs());
		txn.setMsgId(Utils.generateUUID(prefix));
		if (StringUtils.isNotBlank(request.getDirectBillChannel())) {
			txn.setDirectBillChannel(DirectBillChannelType.fromValue(request.getDirectBillChannel()));
		}
		RiskScoresType riskScore = new RiskScoresType();
		RiskScoresType.Score score = new RiskScoresType.Score();
		score.setProvider(orgId);
		score.setType("TXNRISK");
		score.setValue("030");
		riskScore.getScores().add(score);
		txn.setRiskScores(riskScore);
		billFetchRequestType.setTxn(txn);
		CustomerDtlsType custDtls = new CustomerDtlsType();
		custDtls.setMobile(request.getCustomerInfo().getCustomerMobile());
		if (StringUtils.isNotBlank(request.getCustomerInfo().getCustomerEmail())) {
			CustomerDtlsType.Tag c1 = new CustomerDtlsType.Tag();
			c1.setName("EMAIL");
			c1.setValue(request.getCustomerInfo().getCustomerEmail());
			custDtls.getTags().add(c1);
		}
		if (StringUtils.isNotBlank(request.getCustomerInfo().getCustomerAdhaar())) {
			CustomerDtlsType.Tag c1 = new CustomerDtlsType.Tag();
			c1.setName("AADHAAR");
			c1.setValue(request.getCustomerInfo().getCustomerAdhaar());
			custDtls.getTags().add(c1);
		}
		if (StringUtils.isNotBlank(request.getCustomerInfo().getCustomerPan())) {
			CustomerDtlsType.Tag c1 = new CustomerDtlsType.Tag();
			c1.setName("PAN");
			c1.setValue(request.getCustomerInfo().getCustomerPan());
			custDtls.getTags().add(c1);
		}
		billFetchRequestType.setCustomer(custDtls);
		BillDetailsType billdetails = new BillDetailsType();
		BillerType billertype = new BillerType();
		billertype.setId(request.getBillerId());
		billdetails.setBiller(billertype);
		CustomerParamsType custparam = new CustomerParamsType();
		for (int i = 0; i < request.getInputParams().getInput().size(); i++) {
			CustomerParamsType.Tag ct = new CustomerParamsType.Tag();
			ct.setName(request.getInputParams().getInput().get(i).getParamName());
			ct.setValue(request.getInputParams().getInput().get(i).getParamValue());
			custparam.getTags().add(ct);
		}
		billdetails.setCustomerParams(custparam);
		billFetchRequestType.setBillDetails(billdetails);
		AgentType agentType = new AgentType();
		agentType.setId(request.getAgentId());
		DeviceType agentdevice = new DeviceType();
		for (int i = 0; i < request.getAgentDeviceInfo().getTag().size(); i++) {
			DeviceType.Tag tag = new DeviceType.Tag();
			tag.setName(DeviceTagNameType.valueOf(request.getAgentDeviceInfo().getTag().get(i).getName()));
			tag.setValue(request.getAgentDeviceInfo().getTag().get(i).getValue());
			agentdevice.getTags().add(tag);
		}
		agentType.setDevice(agentdevice);
		billFetchRequestType.setAgent(agentType);
		return billFetchRequestType;

	}

	public static BillFetchRequestVO getRequest(String reqStr) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		BillFetchRequestVO reqJson = mapper.readValue(reqStr, BillFetchRequestVO.class);

		return reqJson;
	}

}

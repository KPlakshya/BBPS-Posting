package com.bbps.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.bbps.schema.AdditionalInfoType;
import org.bbps.schema.AgentType;
import org.bbps.schema.AmountType;
import org.bbps.schema.AmtType;
import org.bbps.schema.AnalyticsPaymentTypeInstance;
import org.bbps.schema.AnalyticsType;
import org.bbps.schema.BillDetailsType;
import org.bbps.schema.BillPaymentRequest;
import org.bbps.schema.BillerResponseType;
import org.bbps.schema.BillerType;
import org.bbps.schema.CustomerDtlsType;
import org.bbps.schema.CustomerParamsType;
import org.bbps.schema.DeviceTagNameType;
import org.bbps.schema.DeviceType;
import org.bbps.schema.DirectBillChannelType;
import org.bbps.schema.OffUsPayType;
import org.bbps.schema.PmtMtdType;
import org.bbps.schema.PymntInfType;
import org.bbps.schema.QckPayType;
import org.bbps.schema.RiskScoresType;
import org.bbps.schema.SiTxnType;
import org.bbps.schema.SpltPayType;
import org.bbps.schema.TransactionType;
import org.bbps.schema.TxnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bbps.billpayment.data.BillPaymentRequestVO;
import com.bbps.billpayment.data.BillPaymentResponseVO;
import com.bbps.constants.Constants;
import com.bbps.data.BbpsPostingResponse;
import com.bbps.entity.BillPaymentDetails;
import com.bbps.entity.service.BillPaymentDetailsService;
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

@Service("BillPaymentRequest")
@Slf4j
public class BbpsBillPaymentServiceImpl implements BBPSService {

	@Value("${bbps.orgInst}")
	private String orgId;

	@Value("${bbps.prefix}")
	private String prefix;

	@Autowired
	private BbpsRestConService bbpsRestConService;

	@Autowired
	private CustomerRequestResponseService custReqRespService;

	@Autowired
	private BillPaymentDetailsService billpaymentservice;

	@Override
	public void process(Message message) {
		log.info("inside BbpsBillPaymentServiceImpl process [{}]", message.toString());
		String billPaymentStr = null;
		BbpsPostingResponse bbpspostingresp = null;
		BillPaymentRequest billpayment = null;
		BillPaymentDetails billPaymentDetails = null;

		try {
			BillPaymentRequestVO request = getRequest(message.getBbpsReqinfo().getMessageBody().getBody());
			billpayment = getBBPSXmlRequest(request);
			billPaymentDetails = billpaymentservice.saveBillDetailsPending(billpayment.getHead().getRefId(), request);
			log.info("Saved billPaymentDetails [{}]",billPaymentDetails.getId());
			billPaymentStr = MarshUnMarshUtil.marshal(billpayment).toString();
			bbpspostingresp = bbpsRestConService.send(billPaymentStr, Constants.BILL_PAYMENT_REQUEST,
					billpayment.getHead().getRefId());

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Unable to process bill payment request [{}]", e.getMessage());
			bbpspostingresp = new BbpsPostingResponse();
			bbpspostingresp.setErrorCode(Constants.ERROR_CODE_99);
			bbpspostingresp.setAck(Constants.ERROR_MSG_99);

		} finally {
			String id = message.getBbpsReqinfo().getHeaders().get(Constants.CUSTOMER_REQ_ID).toString();
			String refId = billpayment != null ? billpayment.getHead().getRefId() : null;
			if (bbpspostingresp.getAckerror() != null) {

				BillPaymentResponseVO response = new BillPaymentResponseVO();
				response.setResponseCode(bbpspostingresp.getErrorCode());
				response.setResponseMessage(bbpspostingresp.getAckerror());
				custReqRespService.fetchAndUpdateFailure(id, bbpspostingresp.getHttpcode(), refId, response);

			} else {
				custReqRespService.fetchAndUpdateIntiated(id, bbpspostingresp.getHttpcode(), refId);
				billPaymentDetails.setStatus(Constants.INTIATED);
			}
			log.info("Before Insert Bill Paymnet Details");
			billpaymentservice.update(billPaymentDetails);

			// save in audit table
		}

	}

	private BillPaymentRequest getBBPSXmlRequest(BillPaymentRequestVO request)
			throws JsonMappingException, JsonProcessingException {

		BillPaymentRequest xmlReq = new BillPaymentRequest();
		xmlReq.setHead(Utils.createHead(orgId, prefix));
		xmlReq.getHead().setOrigRefId(request.getOrigRefId());
		xmlReq.getHead().setSiTxn(SiTxnType.fromValue(request.getSiTxn()));
		AnalyticsType analyticsType = new AnalyticsType();
		AnalyticsType.Tag t1 = new AnalyticsType.Tag();
		t1.setName(AnalyticsPaymentTypeInstance.PAYREQUESTSTART.value());
		t1.setValue(Utils.generateTs());
		AnalyticsType.Tag t2 = new AnalyticsType.Tag();
		t2.setName(AnalyticsPaymentTypeInstance.PAYREQUESTEND.value());
		t2.setValue(Utils.generateTs());
		analyticsType.getTags().add(t1);
		analyticsType.getTags().add(t2);
		xmlReq.setAnalytics(analyticsType);

		TxnType txn = new TxnType();
		txn.setTxnReferenceId(request.getTxnReferenceId());
		txn.setType(TransactionType.FORWARD_TYPE_REQUEST.value());
		txn.setTs(Utils.generateTs());
		txn.setMsgId(Utils.generateUUID(prefix));
		if (StringUtils.isNotBlank(request.getDirectBillChannel())) {
			txn.setDirectBillChannel(DirectBillChannelType.fromValue(request.getDirectBillChannel()));
		}
		if (StringUtils.isNotBlank(request.getDirectBillContentId())) {
			txn.setDirectBillContentId(request.getDirectBillContentId());
		}
		RiskScoresType riskScore = new RiskScoresType();
		RiskScoresType.Score score = new RiskScoresType.Score();
		score.setProvider(orgId);
		score.setType("TXNRISK");
		score.setValue("030");
		riskScore.getScores().add(score);
		txn.setRiskScores(riskScore);
		xmlReq.setTxn(txn);
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
		xmlReq.setCustomer(custDtls);
		AgentType agentType = new AgentType();
		agentType.setId(request.getAgentId());
		DeviceType agentdevice = new DeviceType();
		for (int i = 0; i < request.getAgentDeviceInfo().getTag().size(); i++) {
			DeviceType.Tag tag = new DeviceType.Tag();
			tag.setName(DeviceTagNameType.fromValue(request.getAgentDeviceInfo().getTag().get(i).getName()));
			tag.setValue(request.getAgentDeviceInfo().getTag().get(i).getName());
			agentdevice.getTags().add(tag);
		}
		agentType.setDevice(agentdevice);
		xmlReq.setAgent(agentType);
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
		xmlReq.setBillDetails(billdetails);

		BillerResponseType billerResp = new BillerResponseType();
		billerResp.setCustomerName(request.getBillerResponse().getCustomerName());
		billerResp.setAmount(request.getBillerResponse().getAmount());
		billerResp.setDueDate(request.getBillerResponse().getDueDate());
		billerResp.setBillDate(request.getBillerResponse().getBillDate());
		billerResp.setBillNumber(request.getBillerResponse().getBillNumber());
		billerResp.setBillPeriod(request.getBillerResponse().getBillPeriod());

		for (int i = 0; i < request.getBillerResponse().getTags().size(); i++) {
			BillerResponseType.Tag tag = new BillerResponseType.Tag();
			tag.setName(request.getBillerResponse().getTags().get(i).getName());
			tag.setValue(request.getBillerResponse().getTags().get(i).getValue());
			billerResp.getTags().add(tag);

		}
		xmlReq.setBillerResponse(billerResp);
		AdditionalInfoType addinfo = new AdditionalInfoType();
		for (int i = 0; i < request.getAdditionaInfo().size(); i++) {
			AdditionalInfoType.Tag tag = new AdditionalInfoType.Tag();
			tag.setName(request.getAdditionaInfo().get(i).getName());
			tag.setValue(request.getAdditionaInfo().get(i).getValue());
			addinfo.getTags().add(tag);

		}
		xmlReq.setAdditionalInfo(addinfo);

		PmtMtdType pmtMtdType = new PmtMtdType();
		pmtMtdType.setQuickPay(QckPayType.fromValue(request.getPaymentMethod().getQuickPay()));
		pmtMtdType.setSplitPay(SpltPayType.fromValue(request.getPaymentMethod().getSplitPay()));
		pmtMtdType.setOFFUSPay(OffUsPayType.fromValue(request.getPaymentMethod().getOffusPay()));
		pmtMtdType.setPaymentMode(request.getPaymentMethod().getPaymentMode());
		xmlReq.setPaymentMethod(pmtMtdType);

		AmountType amounttype = new AmountType();
		AmtType amtType = new AmtType();
		amtType.setAmount(request.getAmount().getAmt());
		amtType.setCOUcustConvFee(request.getAmount().getCouCustConvFee());
		amtType.setCustConvFee(request.getAmount().getCustConvFee());
		amtType.setCurrency(request.getAmount().getCurrency());
		amounttype.setAmt(amtType);
		amounttype.setSplitPayAmount(request.getAmount().getSplitPayAmount());
//		for (int i = 0; i < request.getAmount().getAmountTags().size(); i++) {
//			AmountType.Tag tag = new AmountType.Tag();
//			tag.setName(request.getAdditionaInfo().get(i).getName());
//			tag.setValue(request.getAdditionaInfo().get(i).getValue());
//			amounttype.getTags().add(tag);
//
//		}
		xmlReq.setAmount(amounttype);
		PymntInfType pymntInfType = new PymntInfType();
//		for (int i = 0; i < request.getPaymentInformationTags().size(); i++) {
//			PymntInfType.Tag tag = new PymntInfType.Tag();
//			tag.setName(request.getAdditionaInfo().get(i).getName());
//			tag.setValue(request.getAdditionaInfo().get(i).getValue());
//			pymntInfType.getTags().add(tag);
//
//		}
		xmlReq.setPaymentInformation(pymntInfType);
		return xmlReq;

	}

	public static BillPaymentRequestVO getRequest(String reqStr) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		BillPaymentRequestVO reqJson = mapper.readValue(reqStr, BillPaymentRequestVO.class);

		return reqJson;
	}

}

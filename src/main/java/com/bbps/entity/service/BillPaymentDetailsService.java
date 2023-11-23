package com.bbps.entity.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bbps.billpayment.data.BillPaymentRequestVO;
import com.bbps.constants.Constants;
import com.bbps.entity.BillPaymentDetails;
import com.bbps.entity.repo.BillPaymentDetailsRepo;

@Service
@Slf4j
public class BillPaymentDetailsService {

	@Autowired
	private BillPaymentDetailsRepo repo;

	public BillPaymentDetails saveBillDetailsPending(String refId, BillPaymentRequestVO jsonRequest) {
		BillPaymentDetails billdtl = new BillPaymentDetails();
		billdtl.setRefId(refId);
		billdtl.setTxnReferenceId(jsonRequest.getTxnReferenceId());
		billdtl.setDirectBillChannel(jsonRequest.getDirectBillChannel());
		billdtl.setDirectBillContentId(jsonRequest.getDirectBillChannel());
		billdtl.setPaymentRefId(jsonRequest.getPaymentRefId());
		billdtl.setCustomerMobile(jsonRequest.getCustomerInfo().getCustomerMobile());
		billdtl.setCustomerEmail(jsonRequest.getCustomerInfo().getCustomerEmail());
		billdtl.setCustomerAdhaar(jsonRequest.getCustomerInfo().getCustomerAdhaar());
		billdtl.setCustomerPan(jsonRequest.getCustomerInfo().getCustomerPan());
		billdtl.setAgentId(jsonRequest.getAgentId());

		Optional.ofNullable(jsonRequest.getAgentDeviceInfo()).ifPresent(f -> {
			String agentDevice = f.getTag().stream().map(m -> {
				StringBuilder builder = new StringBuilder();
				builder.append(m.getName()).append("=").append(m.getValue());
				return builder.toString();
			}).collect(Collectors.joining("~"));
			billdtl.setAgentDeviceInfo(agentDevice);
		});

		billdtl.setBillerId(jsonRequest.getBillerId());

		Optional.ofNullable(jsonRequest.getInputParams()).ifPresent(f -> {
			String inputparam = f.getInput().stream().map(m -> {
				StringBuilder builder = new StringBuilder();
				builder.append(m.getParamName()).append("=").append(m.getParamValue());
				return builder.toString();
			}).collect(Collectors.joining("~"));
			billdtl.setInputParams(inputparam);
		});
		billdtl.setCustomerName(jsonRequest.getBillerResponse().getCustomerName());
		billdtl.setAmount(jsonRequest.getBillerResponse().getAmount());
		billdtl.setDueDate(jsonRequest.getBillerResponse().getDueDate());
		billdtl.setBillDate(jsonRequest.getBillerResponse().getBillDate());
		billdtl.setBillNumber(jsonRequest.getBillerResponse().getBillNumber());
		billdtl.setBillPeriod(jsonRequest.getBillerResponse().getBillPeriod());
		billdtl.setCustConvFee(jsonRequest.getAmount().getCustConvFee());
		billdtl.setCOUcustConvFee(jsonRequest.getAmount().getCouCustConvFee());
		billdtl.setCurrency(jsonRequest.getAmount().getCurrency());
		billdtl.setSplitPayAmount(jsonRequest.getAmount().getSplitPayAmount());

		Optional.ofNullable(jsonRequest.getAmount()).ifPresent(f -> {
			String tags = f.getAmountTags().stream().map(m -> {
				StringBuilder builder = new StringBuilder();
				builder.append(m.getName()).append("=").append(m.getValue());
				return builder.toString();
			}).collect(Collectors.joining("~"));
			billdtl.setAmountTag(tags);
		});

		Optional.ofNullable(jsonRequest.getPaymentInformationTags()).ifPresent(f -> {
			String tags = f.stream().map(m -> {
				StringBuilder builder = new StringBuilder();
				builder.append(m.getName()).append("=").append(m.getValue());
				return builder.toString();
			}).collect(Collectors.joining("~"));
			billdtl.setPaymentInformation(tags);
		});
		billdtl.setPaymentInformation(null);
		billdtl.setRequestTimestamp(Timestamp.valueOf(LocalDateTime.now()));
		billdtl.setStatus(Constants.PENDING);

		log.info("Before Insert into billpaymnetbdetails [{}]",billdtl);
		return repo.save(billdtl);

	}

	public void update(BillPaymentDetails billPaymentDetails) {
		repo.save(billPaymentDetails);

	}

}

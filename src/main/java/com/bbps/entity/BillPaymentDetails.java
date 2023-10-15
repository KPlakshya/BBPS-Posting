package com.bbps.entity;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "Bill_Payement_Details")
@Data
public class BillPaymentDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "ref_id")
	private String refId;

	@Column(name = "txn_reference_id")
	private String txnReferenceId;

	@Column(name = "direct_bill_channel")
	private String directBillChannel;

	@Column(name = "direct_bill_content_id")
	private String directBillContentId;

	@Column(name = "payment_ref_id")
	private String paymentRefId;

	@Column(name = "customer_mobile")
	private String customerMobile;

	@Column(name = "customer_email")
	private String customerEmail;

	@Column(name = "customer_adhaar")
	private String customerAdhaar;

	@Column(name = "customer_pan")
	private String customerPan;

	@Column(name = "agent_id")
	private String agentId;

	@Column(name = "agent_device_info")
	private String agentDeviceInfo;

	@Column(name = "biller_id")
	private String billerId;

	@Column(name = "customer_input_params")
	private String inputParams;

	@Column(name = "customer_name")
	private String customerName;

	@Column(name = "amount")
	private String amount;

	@Column(name = "due_date")
	private String dueDate;

	@Column(name = "bill_date")
	private String billDate;

	@Column(name = "bill_number")
	private String billNumber;

	@Column(name = "bill_period")
	private String billPeriod;

	@Column(name = "cust_conv_fee")
	private String custConvFee;

	@Column(name = "cou_cust_conv_Fee")
	private String cOUcustConvFee;

	@Column(name = "currency")
	private String currency;

	@Column(name = "split_pay_amount")
	private String splitPayAmount;

	@Column(name = "amount_tag")
	private String amountTag;

	@Column(name = "payment_information")
	private String paymentInformation;

	@Column(name = "request_timestamp")
	private Timestamp requestTimestamp;

	@Column(name = "status")
	private String status;
	
	@Column(name = "response_code")
	private String responseCode;
	
	@Column(name = "response_message")
	private String responseMessage;
	
	@Column(name = "compliance_resp_code")
	private String complianceRespCd;
	
	@Column(name = "compliance_reason")
	private String complianceReason;
	
	@Column(name = "approval_ref_num")
	private String approvalRefNum;
	
	@Column(name = "response_timestamp")
	private Timestamp responseTimestamp;
	

}

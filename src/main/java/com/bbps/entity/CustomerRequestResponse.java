package com.bbps.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "Customer_Request_Response")
@Data
public class CustomerRequestResponse implements Serializable{
	
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
	
	@Column(name = "request_type")
    private String requestType;
	
	//BBPS txnId
	@Column(name = "ref_id")
    private String refId;
    
    @Column(name = "request")
    private String request;
    
    @Column(name = "request_timestamp")
    private Timestamp requestTimestamp;
    
    @Column(name = "response")
    private String response;
    
    @Column(name = "reponse_timestamp")
    private Timestamp responseTimestamp;
    
    @Column(name = "request_http_status")
    private String requestHttpStatus;
    
    @Column(name = "status")
    private String status;

}

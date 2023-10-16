package com.bbps.data;

import java.io.Serializable;

import lombok.Data;

@Data
public class BbpsPostingResponse implements Serializable {

	private static final long serialVersionUID = 4279246952333118777L;
	String httpcode;
	String ack;
	String ackerror;
	String errorCode;
}

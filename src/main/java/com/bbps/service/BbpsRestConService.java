package com.bbps.service;

import com.bbps.data.BbpsPostingResponse;

public interface BbpsRestConService {
	public BbpsPostingResponse send(String data, String apiName, String txnId);
}

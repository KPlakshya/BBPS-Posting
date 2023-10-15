package com.bbps.scheduler;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.bbps.schema.ReqDiagnosticType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bbps.constants.Constants;
import com.bbps.service.BbpsRestConService;
import com.bbps.service.Utils;
import com.bbps.utils.MarshUnMarshUtil;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommonScheduler {
	
	@Value("$bbps.orgInst")
	private String orgId;
	
	@Value("$bbps.prefix")
	private String prefix;
	
	@Value("${bbps.close.idle.connection.wait.time}")
	private int closIdletime;

	@Qualifier("poolingHttpClientConnectionManager")
	@Autowired
	private PoolingHttpClientConnectionManager connectionManager;

	@Autowired
	private BbpsRestConService restService;
	

	@Scheduled(initialDelayString = "${npci.cc.reqhbt.initial.delay}", fixedDelayString = "${npci.cc.reqhbt.fixed.delay}")
	public void reqdiagAliveScheduler() {
		log.info("Start REQ_DIAGNOSTIC Scheduler [{}]", LocalDateTime.now());
		
			ReqDiagnosticType req = createReqDiagnostic();
			String reqhbtStr = MarshUnMarshUtil.marshal(req).toString();
			restService.send(reqhbtStr, Constants.REQ_DIAGNOSTIC, req.getHead().getRefId());

		log.info("End REQ_DIAGNOSTIC Scheduler");

	}
	
	private ReqDiagnosticType createReqDiagnostic() {
		ReqDiagnosticType  reqHbt = new ReqDiagnosticType();
		reqHbt.setHead(Utils.createHead(orgId, prefix));
		return reqHbt;
	}

	@Scheduled(initialDelayString = "${bbps.idle.connection.monitor.initialdelay}", fixedDelayString = "${bbps.idle.connection.monitor.fixeddelay}")
	public void closeNpciIdleConn() {
		try {
			log.info(
					"Scheduler Run for NPCI IdleConnectionMonitor - Going to close if any expired and idle connections..");
			connectionManager.closeExpiredConnections();
			connectionManager.closeIdleConnections(closIdletime, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("Scheduler Run for NPCI IdleConnectionMonitor Error {}", e);
		}
	}

}

package com.bbps.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.bbps.schema.Ack;
import org.bbps.schema.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bbps.constants.Constants;
import com.bbps.data.BbpsPostingResponse;
import com.bbps.service.BbpsRestConService;
import com.bbps.utils.SignatureUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BbpsRestConServiceImpl implements BbpsRestConService {

	private static JAXBContext jAXBContextAck = null;

	@Qualifier("closeableHttpClient")
	@Autowired
	private CloseableHttpClient closeableHttpClient;

	@Autowired
	private SignatureUtils signUtils;

	@Value("${bbps.base.url}")
	private String baseUrl;

	@Override
	public BbpsPostingResponse send(String data, String apiName, String refId) {
		BbpsPostingResponse bbpsResponse = new BbpsPostingResponse();
		String signedXml = signUtils.generateXMLDigitalSignature(data);
		if (StringUtils.isBlank(signedXml)) {
			log.error("Signed xml is null for apiName [{}] refId [{}]", apiName, refId);
		}
		StringBuffer outputSB = new StringBuffer();
		Ack ack = null;
		boolean isAck = false;
		long ts = 0;
		try {
			log.info(signedXml);
			String npciUrl = baseUrl + apiName + Constants.CONST_FRD_SLASH + Constants.BBPS_V1
					+ Constants.BBPS_URL_SUFFIX + refId;
			log.info("NPCI URL [{}]", npciUrl);
			HttpPost post = new HttpPost(npciUrl.trim());
			post.setHeader("Content-Type", "application/xml");
			post.setHeader("Accept", "application/xml");
			post.setHeader("cache-control", "no-cache");
			StringEntity params = new StringEntity(signedXml);
			post.setEntity(params);
			ts = System.currentTimeMillis();
			try (CloseableHttpResponse response = closeableHttpClient.execute(post);) {
				Integer responseCode = response.getStatusLine().getStatusCode();
				bbpsResponse.setHttpcode(responseCode.toString());
				log.info("BBPS Time {} ms with httpRespCode {} ", ts, responseCode);
				try (BufferedReader rd = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));) {
					String line = "";
					while ((line = rd.readLine()) != null) {
						outputSB.append(line);
					}
				}
				ts = System.currentTimeMillis() - ts;
				if (outputSB.toString().trim().isEmpty() || Constants.HTTP_STATUS_200 != responseCode) {
					log.error("ACK NOT RECEIVED And HttpStatusCode {}", responseCode);
				} else {
					isAck = true;
					log.info("Got Acknowledge Txn Id{} Ack{} ", refId, outputSB.toString());
				}

			}
		} catch (org.apache.http.conn.ConnectionPoolTimeoutException e) {
			ts = System.currentTimeMillis() - ts;
			log.error("FOR NPCI HTTP CONNECTION_POOL_TIMEOUT_EXCEPTION msg={} ,error={}", e.getMessage(), e);
		} catch (org.apache.http.conn.HttpHostConnectException e) {
			ts = System.currentTimeMillis() - ts;
			log.error("NPCI HOST_CONNECT_EXCEPTION msg={} ,error={}", e.getMessage(), e);
		} catch (java.net.SocketTimeoutException e) {
			ts = System.currentTimeMillis() - ts;
			log.error("NPCI SOCKET_TIMEOUT_EXCEPTION msg={} ,error={}", e.getMessage(), e);
		} catch (Exception e) {
			ts = System.currentTimeMillis() - ts;
			log.error("error {}", e);
		}
		try {
			if (!isAck) {
				log.error("no ack {} ", refId);
			} else {
				ack = ackUnmarshal(outputSB.toString(), bbpsResponse);
				log.info("Ack Message Receive from BBPS {}", ack.toString());
				if (null != ack.getErrorMessages() || 0 < ack.getErrorMessages().size()) {
					List<ErrorMessage> errorMessages = ack.getErrorMessages();
					log.error("Error in Ack");
					bbpsResponse.setAckerror(errorMessages.get(0).getErrorDtl());
					bbpsResponse.setErrorCode(Constants.ERROR_CODE_01);
					for (ErrorMessage errorMessage : errorMessages) {
						log.error("Error code {}", errorMessage.getErrorCd());
						log.error("Error code {}", errorMessage.getErrorDtl());
					}
				}
			}
		} catch (Exception e) {
			log.error("error :{}", e);
		}
		bbpsResponse.setAck(outputSB.toString());
		return bbpsResponse;

	}

	public static Ack ackUnmarshal(String xmlStr, BbpsPostingResponse bbpsresponse) {
		try (StringReader sr = new StringReader(xmlStr);) {
			if (jAXBContextAck == null) {
				jAXBContextAck = JAXBContext.newInstance(Ack.class);
			}
			Unmarshaller unmarshaller = jAXBContextAck.createUnmarshaller();
			Object obj = unmarshaller.unmarshal(sr);
			return (Ack) obj;
		} catch (Exception e) {
			bbpsresponse.setAckerror(Constants.ERROR_MSG_99);
			bbpsresponse.setErrorCode(Constants.ERROR_CODE_99);
			log.error("error while Unmarsheling  ack from npci{}", e);
		}
		return null;
	}

}

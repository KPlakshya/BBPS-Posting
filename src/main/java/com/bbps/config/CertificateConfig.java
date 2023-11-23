package com.bbps.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.bbps.constants.Constants;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Lazy(false)
@Slf4j
public class CertificateConfig {

	@Value("${bbps.cu.pub.key.path}")
	private String pubFile;

	@Value("${bbps.pvt.key.path}")
	private String pvtFile;

	@Value("${bbps.pvt.key.type}")
	private String pvtType;

	@Value("${bbps.pvt.key.password}")
	private String pvtPass;

	@Value("${bbps.pvt.key.alais}")
	private String pvtAlias;

	@Bean(name = Constants.PVT_KEY)
	public PrivateKey upiPrivateKey() throws Exception {
		return getPrivateKey();
	}

	@Bean(name = Constants.PUB_KEY)
	public PublicKey upiNpciPublicKey() throws Exception {
		return getCertificate();
	}

	private PrivateKey getPrivateKey() throws Exception {
		InputStream in = null;
		try {
			KeyStore keystore = KeyStore.getInstance(pvtType);
			in = new FileInputStream(new File(pvtFile));
			keystore.load(in, pvtPass.toCharArray());
			PrivateKey key = (PrivateKey) keystore.getKey(pvtAlias, pvtPass.toCharArray());
			log.info("private key loaded {}", key);
			return key;
		} catch (Exception e) {
			log.error("Error while loading Private Key ", e);
		} finally {
			if (in != null)
				in.close();
		}
		return null;
	}

	private PublicKey getCertificate() throws Exception {
		CertificateFactory cf = CertificateFactory.getInstance(Constants.X509);
		InputStream in = new FileInputStream(new File(pubFile));
		InputStream caInput = new BufferedInputStream(in);
		Certificate ca;
		try {
			ca = cf.generateCertificate(caInput);
			log.info("public key loaded");
			return ca.getPublicKey();
		} finally {
			try {
				caInput.close();
			} catch (IOException e) {
				log.error("Error while loading public Key 1", e);
			}
			try {
				in.close();
			} catch (IOException e) {
				log.error("Error while loading public Key 2", e);
			}
		}
	}

}

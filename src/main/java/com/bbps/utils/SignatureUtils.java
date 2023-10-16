package com.bbps.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.bbps.constants.Constants;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SignatureUtils {

	@Autowired
	@Qualifier(Constants.PVT_KEY)
	private PrivateKey privateKey;

	@Autowired
	@Qualifier(Constants.PUB_KEY)
	private PublicKey publicKey;

	public String generateXMLDigitalSignature(String unsignedXMLString) {
		try {
			Document doc = getXmlDocumentFromString(unsignedXMLString);

			XMLSignatureFactory xmlSigFactory = XMLSignatureFactory.getInstance(Constants.DOM);
			DOMSignContext domSignCtx = new DOMSignContext(privateKey, doc.getDocumentElement());
			Reference ref = null;
			SignedInfo signedInfo = null;
			ref = xmlSigFactory.newReference("", xmlSigFactory.newDigestMethod(DigestMethod.SHA256, null),
					Collections.singletonList(
							xmlSigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
					null, null);
			log.info("xmlSigFactory.getProvider():{}", xmlSigFactory.getProvider());
			signedInfo = xmlSigFactory.newSignedInfo(
					xmlSigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
							(C14NMethodParameterSpec) null),
					xmlSigFactory.newSignatureMethod(Constants.SIGN_ALG, (SignatureMethodParameterSpec) null),
					Collections.singletonList(ref));
			KeyInfo keyInfo = getKeyInfo(xmlSigFactory);
			XMLSignature xmlSignature = xmlSigFactory.newXMLSignature(signedInfo, keyInfo);
			xmlSignature.sign(domSignCtx);
			return convertXMLToString(doc);
		} catch (Exception e) {
			log.error("error in signing Xml doc {}", e);
		}
		return null;

	}

	public static Document getXmlDocumentFromString(String xmlString) {
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		try {
			doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xmlString)));
		} catch (ParserConfigurationException ex) {
			log.error("Error in converting xm to DOC ParserConfigurationException", ex.getMessage());
		} catch (SAXException ex) {
			log.error("Error in converting xm to DOC SAXException", ex.getMessage());
		} catch (IOException ex) {
			log.error("Error in converting xm to DOC IOException", ex.getMessage());
		}
		return doc;
	}

	private KeyInfo getKeyInfo(XMLSignatureFactory xmlSigFactory) throws Exception {
		KeyInfo keyInfo = null;
		KeyValue keyValue = null;
		KeyInfoFactory keyInfoFact = xmlSigFactory.getKeyInfoFactory();
		keyValue = keyInfoFact.newKeyValue(publicKey);
		keyInfo = keyInfoFact.newKeyInfo(Collections.singletonList(keyValue));
		return keyInfo;
	}
	
	public static String convertXMLToString(Document doc) {
		String result = "";
		try {
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer trans = null;
			trans = transFactory.newTransformer();
			StringWriter writer = new StringWriter();
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, Constants.YES_SMALL);
			trans.transform(new DOMSource(doc), new StreamResult(writer));
			result = writer.getBuffer().toString().replaceAll(Constants.CONST_ESCAPE_CHARS, Constants.CONST_BLANK);
		} catch (TransformerException e) {
			log.error("error :{}", e.getMessage());
		}
		return result;
	}
	

}

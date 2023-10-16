package com.bbps.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarshUnMarshUtil {

	private static final ConcurrentMap<Class, JAXBContext> jaxbContexts = new ConcurrentHashMap<Class, JAXBContext>();

	private final static JAXBContext getJaxbContext(Class clazz) {
		Assert.notNull(clazz, "'clazz' must not be null");
		JAXBContext jaxbContext = jaxbContexts.get(clazz);
		if (jaxbContext == null) {
			try {
				log.info("Creating new instance of {}", clazz);
				jaxbContext = JAXBContext.newInstance(clazz);
				jaxbContexts.putIfAbsent(clazz, jaxbContext);
			} catch (JAXBException ex) {
				throw new HttpMessageConversionException(
						"Could not instantiate JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
			}
		}
		return jaxbContext;
	}

	public static <T> StringWriter marshal(T t) {
		StringWriter writer = new StringWriter();
		try {
			JAXBContext jaxbContext = getJaxbContext(ClassUtils.getUserClass(t));
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			jaxbMarshaller.marshal(t, writer);
		} catch (JAXBException e) {
			log.error("error in marshal :{}", e);
		}

		return writer;

	}

	public static <T> T unmarshal(String xmlStr, Class<T> t) throws Exception {
		try (StringReader sr = new StringReader(xmlStr);) {
			JAXBContext jAXBContext = getJaxbContext(ClassUtils.getUserClass(t));
			jAXBContext = JAXBContext.newInstance(t);
			Unmarshaller unmarshaller = jAXBContext.createUnmarshaller();
			Object obj = unmarshaller.unmarshal(sr);
			return (T) obj;
		} catch (Exception e) {
			log.error("error while Unmarsheling {}", e);
			log.error("error while Unmarshalling apiClass={} ,errorMsg={} ,xmlStr={} ", t.getName(), e.getMessage(),
					xmlStr);
			throw e;
		}
	}

}

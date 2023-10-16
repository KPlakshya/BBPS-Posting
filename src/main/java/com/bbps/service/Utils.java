package com.bbps.service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.bbps.schema.HeadType;

import com.bbps.constants.Constants;

public class Utils {

	public static String generateUUID(String prefix) {
		return prefix + UUID.randomUUID().toString().replaceAll(Constants.CONST_SPCL_HIFN, Constants.CONST_BLANK);
	}

	public static HeadType createHead(String orgId, String prefix) {
		HeadType headType = new HeadType();
		headType.setRefId(Utils.generateUUID(prefix));
		headType.setVer(Constants.BBPS_V1);
		headType.setOrigInst(orgId);
		headType.setTs(Utils.generateTs());
		return headType;
	}
	
	public static String generateTs() {
		DateTimeFormatter dtime = DateTimeFormatter.ofPattern(Constants.ISO_TIMEZONE);
		return ZonedDateTime.now().format(dtime);
	}

}

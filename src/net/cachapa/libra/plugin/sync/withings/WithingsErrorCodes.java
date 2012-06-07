package net.cachapa.libra.plugin.sync.withings;

import java.util.HashMap;
import java.util.Map;

public class WithingsErrorCodes {
	private static final String defaultMessage = "An unknown error occured (error code %s)";
	
	private static final Map<String, String> errorMap = new HashMap<String, String>();
	static {
		errorMap.put("247", "The userid provided is absent, or incorrect");
		errorMap.put("250", "The provided userid and/or Oauth credentials do not match");
		errorMap.put("286", "No such subscription was found");
		errorMap.put("293", "The callback URL is either absent or incorrect");
		errorMap.put("294", "No such subscription could be deleted");
		errorMap.put("304", "The comment is either absent or incorrect");
		errorMap.put("305", "Too many notifications are already set");
		errorMap.put("343", "No notification matching the criteria was found");
		errorMap.put("2555", "An unknown error occured");
	}
	
	public static String getMessage(String errorCode) {
		if (errorMap.containsKey(errorCode)) {
			return errorMap.get(errorCode);
		}
		return String.format(defaultMessage, errorCode);
	}
}

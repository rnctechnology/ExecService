package com.rnctech.nrdataservice.common;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Messages {
	private static final String BUNDLE_NAME = "messages";
	private static final Logger logger = LoggerFactory.getLogger(Messages.class);
	private static Pattern pattern = Pattern.compile("%(\\d+\\$)?(\\(|#| |-|\\+|0|,)?(\\d*)[a-zA-Z]");

	private static String tryReformat(String format, String key, Object... args) {
		try {
			StringBuffer sb = new StringBuffer(key + ":\t");
			Matcher matcher = pattern.matcher(format);
			for (int i = 0; matcher.find(); i++)
				matcher.appendReplacement(sb, "{" + i + "}");
			return MessageFormat.format(sb.toString(), args);
		} catch (Exception ex) {
			return key+" "+ex.getMessage();
		}
	}

	public static String getMessage(String key, Object... args) {
		try {
			ResourceBundle rb = ResourceBundle.getBundle(BUNDLE_NAME);
			return String.format("%s:\t%s", key, String.format(rb.getString(key), args));
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
			return e.getLocalizedMessage();
		}
	}

	public static String getRawMessage(String key) {
		try {
			ResourceBundle rb = ResourceBundle.getBundle(BUNDLE_NAME);
			return rb.getString(key);
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
			return e.getLocalizedMessage();
		}
	}
}

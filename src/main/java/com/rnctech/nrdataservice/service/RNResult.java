package com.rnctech.nrdataservice.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.rnctech.nrdataservice.utils.RNCOutputStream;

/*
* @author zilin chen
* @since 2020.09
*/

public class RNResult {

	public final static String LOG_FILTER = "path resource file://";
	public final static String LOG_FILTER2 = "- Copying";
	
	public static enum Code {
		INITED, PROCESSING, QUEUED, ERROR, FAILED, SUCCESS, INCOMPLETE, COMPLETED, CANCELLED
	}

	public static enum Type {
		HTML, JSON, XML, TABLE, IMG, TEXT, SVG, NULL
	}

	protected Code retCode;
	protected Type retType;
	protected String msg = "";
	protected int sessionid = -1;
	protected List<ResultMessage> messages = new ArrayList<>();

	public RNResult(Code code, String msg) {
		this.retCode = code;
		this.msg = msg;
	}

	public RNResult(Code code, Type type, String msg) {
		this.retCode = code;
		this.retType = type;
		this.msg = msg;
	}

	public RNResult(Code code, List<ResultMessage> msgs) {
		this.retCode = code;
		this.messages = msgs;
	}

	public Code code() {
		return retCode;
	}
	
	public int getSessionid() {
		return sessionid;
	}

	public void setSessionid(int sessionid) {
		this.sessionid = sessionid;
	}

	public void toResultMessage(List<String> logs) {
		logs.stream().forEach(l -> {if(!l.isEmpty() && !l.contains(LOG_FILTER)  && !l.contains(LOG_FILTER2)) messages.add(new ResultMessage(Type.TEXT, l));});
	}

	public void resetResMsg(List<String> logs) {
		messages.clear();
		logs.stream().forEach(l -> {if(!l.isEmpty() && !l.startsWith("2020-") && !l.contains("20/")) messages.add(new ResultMessage(Type.TEXT, l));});
	}
	
	public Code getRetCode() {
		return retCode;
	}

	public void setRetCode(Code retCode) {
		this.retCode = retCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Type getRetType() {
		return retType;
	}

	public void setRetType(Type retType) {
		this.retType = retType;
	};

	transient Logger logger = LoggerFactory.getLogger(RNResult.class);
	private static final Gson gson = new Gson();

	public RNResult(Code code) {
		this.retCode = code;
	}

	public void add(String msg) {
		RNCOutputStream out = new RNCOutputStream(null);
		try {
			out.write(msg);
			out.flush();
			this.messages.addAll(out.toResultMessages());
			out.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

	public void add(Type type, String data) {
		messages.add(new ResultMessage(type, data));
	}

	public void add(ResultMessage interpreterResultMessage) {
		messages.add(interpreterResultMessage);
	}

	public List<ResultMessage> message() {
		return messages;
	}

	public String toJson() {
		return gson.toJson(this);
	}

	public static RNResult fromJson(String json) {
		return gson.fromJson(json, RNResult.class);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(msg+"\n");
		Type prevType = Type.TEXT;
		for (ResultMessage m : messages) {
			if (prevType != null) {
				sb.append("\n");
				if (prevType == Type.TABLE) {
					sb.append("\n");
				}
			}
			sb.append(m.getData());
			prevType = m.getType();
		}

		return sb.toString();
	}

	public static class ResultMessage implements Serializable {
		Type type;
		String data;

		public ResultMessage(Type type, String data) {
			this.type = type;
			this.data = data;
		}

		public Type getType() {
			return type;
		}

		public String getData() {
			return data;
		}

		public String toString() {
			return "%" + type.name().toLowerCase() + " " + data;
		}

		public static final String EXCEEDS_LIMIT_ROWS = "<strong>Output is truncated</strong> to %s rows. Learn more about <strong>%s</strong>";
		public static final String EXCEEDS_LIMIT_SIZE = "<strong>Output is truncated</strong> to %s bytes. Learn more about <strong>%s</strong>";
		public static final String EXCEEDS_LIMIT = "<div class=\"result-alert alert-warning\" role=\"alert\">"
				+ "<button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\">"
				+ "<span aria-hidden=\"true\">&times;</span></button>" + "%s" + "</div>";

		public static ResultMessage getExceedsLimitRowsMessage(int amount, String variable) {
			ResultMessage message = new ResultMessage(Type.HTML,
					String.format(EXCEEDS_LIMIT, String.format(EXCEEDS_LIMIT_ROWS, amount, variable)));
			return message;
		}

		public static ResultMessage getExceedsLimitSizeMessage(int amount, String variable) {
			ResultMessage message = new ResultMessage(Type.HTML,
					String.format(EXCEEDS_LIMIT, String.format(EXCEEDS_LIMIT_SIZE, amount, variable)));
			return message;
		}
	}

	public static class ResultCompletion {
		public String name;
		public String value;
		public String meta;

		public ResultCompletion() {
		}

		public ResultCompletion(String name, String value, String meta) {
			this();
			this.name = name;
			this.value = value;
			this.meta = meta;
		}

		public ResultCompletion(ResultCompletion other) {
			if (other.isSetName()) {
				this.name = other.name;
			}
			if (other.isSetValue()) {
				this.value = other.value;
			}
			if (other.isSetMeta()) {
				this.meta = other.meta;
			}
		}

		public ResultCompletion deepCopy() {
			return new ResultCompletion(this);
		}

		public void clear() {
			this.name = null;
			this.value = null;
			this.meta = null;
		}

		public String getName() {
			return this.name;
		}

		public ResultCompletion setName(String name) {
			this.name = name;
			return this;
		}

		public void unsetName() {
			this.name = null;
		}

		public boolean isSetName() {
			return this.name != null;
		}

		public void setNameIsSet(boolean value) {
			if (!value) {
				this.name = null;
			}
		}

		public String getValue() {
			return this.value;
		}

		public ResultCompletion setValue(String value) {
			this.value = value;
			return this;
		}

		public void unsetValue() {
			this.value = null;
		}

		public boolean isSetValue() {
			return this.value != null;
		}

		public void setValueIsSet(boolean value) {
			if (!value) {
				this.value = null;
			}
		}

		public String getMeta() {
			return this.meta;
		}

		public ResultCompletion setMeta(String meta) {
			this.meta = meta;
			return this;
		}

		public void unsetMeta() {
			this.meta = null;
		}

		public boolean isSetMeta() {
			return this.meta != null;
		}

		public void setMetaIsSet(boolean value) {
			if (!value) {
				this.meta = null;
			}
		}
	}

}

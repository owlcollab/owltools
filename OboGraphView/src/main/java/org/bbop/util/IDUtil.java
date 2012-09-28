package org.bbop.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class IDUtil {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(IDUtil.class);

	public static class VariableValue {
		protected IDUtil.Variable v;

		protected String value;

		public VariableValue(IDUtil.Variable v, String value) {
			this.v = v;
			this.value = value;
		}

		public IDUtil.Variable getVariable() {
			return v;
		}

		public String getValue() {
			return value;
		}
	}

	public static class Variable {
		protected String name;

		protected java.util.List<String> params = new ArrayList<String>();

		public Variable(String s) {
			this.name = s;
		}

		public String getName() {
			return name;
		}

		public void addParam(String param) {
			params.add(param);
		}

		public java.util.List<String> getParams() {
			return params;
		}

		@Override
		public String toString() {
			return "[variable: " + name + ", params: " + params + "]";
		}
	}

	public static List<?> parseVarString(String s) {
		List<Object> out = new ArrayList<Object>();
		if (s == null)
			return out;
		StringBuffer buffer = new StringBuffer();
		boolean inVar = false;
		boolean inParens = false;
		boolean inQuotes = false;
		IDUtil.Variable currentVariable = null;
		for (int i = 0; i < s.length(); i++) {
			if (!inParens && s.charAt(i) == '$') {
				if (inVar) {
					if (currentVariable == null)
						currentVariable = new IDUtil.Variable(buffer.toString());
					out.add(currentVariable);
					inVar = false;
					inParens = false;
					currentVariable = null;
				} else {
					out.add(buffer.toString());
					inVar = true;
					inParens = false;
				}
				buffer = new StringBuffer();
			} else if (inVar && !inParens && s.charAt(i) == '(') {
				currentVariable = new IDUtil.Variable(buffer.toString());
				buffer = new StringBuffer();
				inParens = true;
			} else if (inVar && inParens && s.charAt(i) == ')') {
				currentVariable.addParam(buffer.toString().trim());
				buffer = new StringBuffer();
				inParens = false;
			} else if (inVar && inParens && s.charAt(i) == ',') {
				currentVariable.addParam(buffer.toString().trim());
				buffer = new StringBuffer();
			} else if (s.charAt(i) == '\\') {
				if (i + 1 < s.length()
						&& (s.charAt(i + 1) == '$' || s.charAt(i + 1) == ')'
							|| s.charAt(i + 1) == '(' || s.charAt(i + 1) == ',')) {
					i++;
					buffer.append('$');
				}
			} else
				buffer.append(s.charAt(i));
		}
		if (inVar || inParens || inQuotes || currentVariable != null)
			return null;
		if (buffer.length() > 0)
			out.add(buffer.toString());
		return out;
	}


	public static boolean isLegalID(String id) {
		for (int i = 0; i < id.length(); i++)
			if (Character.isWhitespace(id.charAt(i)))
				return false;
		return true;
	}


}

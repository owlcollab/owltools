package owltools.gaf.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IssueListener {
	
	public void reportIssue(String id, String msg, boolean fatal);
	
	
	public static class DefaultIssueListener implements IssueListener {

		private List<String> errors = null;
		private List<String> warnings = null;
		private Map<String, String> mappedErrors = null;
		private Map<String, String> mappedWarnings = null;
		
		@Override
		public void reportIssue(String id, String msg, boolean fatal) {
			if (id == null) {
				if (fatal) {
					errors = assertInitialized(errors);
					errors.add(msg);
				}
				else {
					warnings = assertInitialized(warnings);
					warnings.add(msg);
				}
			}
			else {
				if (fatal) {
					mappedErrors = assertInitialized(mappedErrors);
					mappedErrors.put(id, msg);
				}
				else {
					mappedWarnings = assertInitialized(mappedWarnings);
					mappedWarnings.put(id, msg);
				}
			}
		}
		
		private static List<String> assertInitialized(List<String> list) {
			if (list == null) {
				list = new ArrayList<String>();
			}
			return list;
		}
		
		private static Map<String, String> assertInitialized(Map<String, String> map) {
			if (map == null) {
				map = new HashMap<String, String>();
			}
			return map;
		}

		public List<String> getErrors() {
			return errors;
		}

		public List<String> getWarnings() {
			return warnings;
		}

		public Map<String, String> getMappedErrors() {
			return mappedErrors;
		}

		public Map<String, String> getMappedWarnings() {
			return mappedWarnings;
		}
	}
}
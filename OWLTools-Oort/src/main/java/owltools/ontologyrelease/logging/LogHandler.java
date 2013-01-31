package owltools.ontologyrelease.logging;

public interface LogHandler {
	
	 public void logInfo(String msg);
	 
	 public void logWarn(String msg, Throwable e);
	 
	 public void logError(String msg, Throwable e);
	 
	 public void report(String reportName, CharSequence content);
}
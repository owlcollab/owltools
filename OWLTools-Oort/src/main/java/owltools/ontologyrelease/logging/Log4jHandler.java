package owltools.ontologyrelease.logging;

import org.apache.log4j.Logger;

public class Log4jHandler implements LogHandler {
	
	private final Logger logger;
	private final boolean printReports;

	/**
	 * @param logger
	 * @param printReports
	 */
	public Log4jHandler(Logger logger, boolean printReports) {
		this.logger = logger;
		this.printReports = printReports;
	}

	@Override
	public void logInfo(String msg) {
		logger.info(msg);
	}

	@Override
	public void logWarn(String msg, Throwable e) {
		if (e != null) {
			logger.warn(msg, e);
		}
		else {
			logger.warn(msg);
		}
	}

	@Override
	public void logError(String msg, Throwable e) {
		if (e != null) {
			logger.error(msg, e);
		}
		else {
			logger.error(msg);
		}
	}

	@Override
	public void report(String reportName, CharSequence content) {
		if (printReports) {
			logger.info(content);
		}
	}

}

package owltools.ontologyrelease.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

public class GuiLogHandler implements LogHandler {

	// SimpleDateFormat is NOT thread safe
	// encapsulate as thread local
	private final static ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>(){

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};
	
	private final BlockingQueue<String> logQueue;
	
	/**
	 * @param logQueue
	 */
	public GuiLogHandler(BlockingQueue<String> logQueue) {
		this.logQueue = logQueue;
	}

	@Override
	public void logInfo(String msg) {
		append("INFO", msg, true);
	}

	@Override
	public void logWarn(String msg, Throwable e) {
		append("WARN", msg, true);
	}

	@Override
	public void logError(String msg, Throwable e) {
		append("ERROR", msg, true);
	}

	private void append(String level, CharSequence msg, boolean printTime) {
		try {
			StringBuilder sb = new StringBuilder();
			if (printTime) {
				sb.append(df.get().format(new Date()));
			}
			sb.append(' ');
			sb.append(level);
			sb.append(' ');
			sb.append(msg);
			logQueue.put(sb.toString());
		} catch (InterruptedException e) {
			Logger.getLogger(GuiLogHandler.class).fatal("Interruped during wait for writing to the message panel.", e);
		}
	}

	@Override
	public void report(String reportName, CharSequence content) {
		append("REPORT: "+reportName+"\n", content, false);
		
	}
	
}

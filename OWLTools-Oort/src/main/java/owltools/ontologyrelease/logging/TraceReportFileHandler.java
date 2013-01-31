package owltools.ontologyrelease.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class TraceReportFileHandler implements LogHandler {
	
	private final File traceReportFile;

	public TraceReportFileHandler(File base, String errorReportName) throws IOException {
		traceReportFile = new File(base, errorReportName);
		try {
			traceReportFile.getParentFile().mkdirs();
			FileUtils.write(traceReportFile, ""); // empty report file
		} catch (IOException e) {
			Logger.getLogger(TraceReportFileHandler.class).error("Could not prepare error report file: "+traceReportFile.getAbsolutePath(), e);
			throw e; 
		}
	}

	@Override
	public void logInfo(String msg) {
		append("INFO", msg, null);
	}

	@Override
	public void logWarn(String msg, Throwable t) {
		append("WARN", msg, t);
	}

	@Override
	public void logError(String msg, Throwable t) {
		append("ERROR", msg, t);
	}

	@Override
	public void report(String reportName, CharSequence content) {
		append(reportName, content, null);
	}

	private void append(CharSequence type, CharSequence content, Throwable t) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileOutputStream(traceReportFile, true));
			pw.print(new Date());
			pw.print('\t');
			pw.print(type);
			pw.print('\t');
			pw.println(content);
			if (t != null) {
				t.printStackTrace(pw);
			}
		} catch (IOException exception) {
			Logger.getLogger(TraceReportFileHandler.class).error("Could not append report to error report: "+traceReportFile.getAbsolutePath(), exception);
		}
		finally {
			IOUtils.closeQuietly(pw);
		}
	}
}

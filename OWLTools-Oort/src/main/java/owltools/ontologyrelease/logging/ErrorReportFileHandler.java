package owltools.ontologyrelease.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class ErrorReportFileHandler implements LogHandler {
	
	private final File base;
	private final File errorReportFile;

	public ErrorReportFileHandler(File base, String errorReportName) {
		this.base = base;
		if (errorReportName != null) {
			errorReportFile = new File(base, errorReportName);
			try {
				errorReportFile.getParentFile().mkdirs();
				FileUtils.write(errorReportFile, ""); // empty report file
			} catch (IOException e) {
				Logger.getLogger(ErrorReportFileHandler.class).error("Could not prepare error report file: "+errorReportFile.getAbsolutePath(), e);
			}
		}
		else {
			errorReportFile = null;
		}
	}

	@Override
	public void logInfo(String msg) {
		// do nothing
	}

	@Override
	public void logWarn(String msg, Throwable e) {
		// do nothing
	}

	@Override
	public void logError(String msg, Throwable t) {
		if (errorReportFile != null) {
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new FileOutputStream(errorReportFile, true));
				pw.println(msg);
				if (t != null) {
					t.printStackTrace(pw);
				}
			} catch (IOException exception) {
				Logger.getLogger(ErrorReportFileHandler.class).error("Could not append report to error report: "+errorReportFile.getAbsolutePath(), exception);
			}
			finally {
				IOUtils.closeQuietly(pw);
			}
		}
	}

	@Override
	public void report(String reportName, CharSequence content) {
		try {
			File file = new File(base, reportName);
			file.getParentFile().mkdirs();
			FileUtils.write(file, content);
		} catch (IOException e) {
			Logger.getLogger(ErrorReportFileHandler.class).error("Could not write report to file: "+reportName, e);
		}
	}

}

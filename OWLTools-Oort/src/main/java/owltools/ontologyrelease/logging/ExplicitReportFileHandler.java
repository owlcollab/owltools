package owltools.ontologyrelease.logging;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import owltools.ontologyrelease.OortConfiguration;
import owltools.ontologyrelease.ReleaseRunnerFileTools;

public abstract class ExplicitReportFileHandler implements LogHandler {

	public static LogHandler createNameFiltered(final Set<String> names, OortConfiguration config) {
		return new ExplicitReportFileHandler(config) {
			
			@Override
			protected boolean doWriteReport(String reportName) {
				return names.contains(reportName);
			}
		};
	}
	
	public static LogHandler createSuffixFiltered(final Set<String> suffixes, OortConfiguration config) {
		return new ExplicitReportFileHandler(config) {
			
			@Override
			protected boolean doWriteReport(String reportName) {
				for (String suffix : suffixes) {
					if (reportName.endsWith(suffix)) {
						return true;
					}
				}
				return false;
			}
		};
	}
	
	private final OortConfiguration config;
	
	protected ExplicitReportFileHandler(OortConfiguration config) {
		this.config = config;
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
	public void logError(String msg, Throwable e) {
		// do nothing
	}

	@Override
	public void report(String reportName, CharSequence content) {
		if (doWriteReport(reportName)) {
			writeReportFile(reportName, content);
		}
	}

	protected abstract boolean doWriteReport(String reportName);
	
	private void writeReportFile(String reportName, CharSequence content) {
		try {
			File outputFolder = config.getBase();
			if (config.isVersionReportFiles()) {
				outputFolder = new File(outputFolder, ReleaseRunnerFileTools.STAGING_DIRECTORY_NAME);
			}
			FileUtils.write(new File(outputFolder, reportName), content);
		} catch (IOException e) {
			Logger.getLogger(ExplicitReportFileHandler.class).error("Could not write report: "+reportName, e);
		}
	}
}

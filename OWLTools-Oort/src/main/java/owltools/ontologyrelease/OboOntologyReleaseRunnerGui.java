package owltools.ontologyrelease;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import owltools.ontologyrelease.gui.OortGuiConfiguration;
import owltools.ontologyrelease.gui.OortGuiMainFrame;
import owltools.ontologyrelease.logging.ErrorReportFileHandler;
import owltools.ontologyrelease.logging.GuiLogHandler;
import owltools.ontologyrelease.logging.LogHandler;
import owltools.ontologyrelease.logging.TraceReportFileHandler;

/**
 * GUI access for ontology release runner.
 */
public class OboOntologyReleaseRunnerGui {

	public static void main(String[] args) throws IOException {
		
		// parse command-line
		OortGuiConfiguration parameters = new OortGuiConfiguration();  
		boolean isHelp = OboOntologyReleaseRunner.parseOortCommandLineOptions(args, parameters);
		if (isHelp) {
			return;
		}
		
		// setup logger for GUI
		final List<LogHandler> handlers = new ArrayList<LogHandler>();
		
		final BlockingQueue<String> logQueue =  new ArrayBlockingQueue<String>(100);
		handlers.add(new GuiLogHandler(logQueue));
		
		
		// setup additional log handlers
		if (parameters.getErrorReportFile() != null) {
			handlers.add(new ErrorReportFileHandler(parameters.getBase(), parameters.getErrorReportFile()));
		}
		if (parameters.getTraceReportFile() != null) {
			handlers.add(new TraceReportFileHandler(parameters.getBase(), parameters.getTraceReportFile()));
		}
		
		// Start GUI
		new ReleaseGuiMainFrameRunner(logQueue, parameters, handlers);
	}

	private static final class ReleaseGuiMainFrameRunner extends OortGuiMainFrame {
		
		// generated
		private static final long serialVersionUID = -8690322825608397262L;
		private final List<LogHandler> handlers;
		
		private ReleaseGuiMainFrameRunner(BlockingQueue<String> logQueue, OortGuiConfiguration parameters, List<LogHandler> handlers) {
			super(logQueue, parameters);
			this.handlers = handlers;
		}
	
		@Override
		protected void executeRelease(final OortGuiConfiguration parameters) {
			logInfo("Starting release manager process");
			disableReleaseButton();
			// execute the release in a separate Thread, otherwise the GUI might be blocked.
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						File base = parameters.getBase().getCanonicalFile();
						OboOntologyReleaseRunner oorr = new OboOntologyReleaseRunner(parameters, base, handlers) {

							@Override
							protected boolean allowFileOverwrite(File file) throws IOException {
								String message = "The release manager will overwrite existing files. Do you want to allow this?";
								String title = "Allow file overwrite?";
								int answer = JOptionPane.showConfirmDialog(ReleaseGuiMainFrameRunner.this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
								boolean allowOverwrite = answer == JOptionPane.YES_OPTION;
								ReleaseGuiMainFrameRunner.this.getAdvancedPanel().setAllowOverwrite(allowOverwrite);
								oortConfig.setAllowFileOverWrite(allowOverwrite);
								return allowOverwrite;
							}

							@Override
							boolean forceLock(File file) {
								JLabel label = new JLabel("<html><p><b>WARNING:</b></p>"
										+"<p>The release manager was not able to lock the staging directory:</p>"
										+"<p>"+file.getAbsolutePath()+"</p><br/>"
										+"<div align=\"center\"><b>Do you want to force this?</b></div><br/></html>");
								String title = "Force lock for staging directory";
								int answer = JOptionPane.showConfirmDialog(ReleaseGuiMainFrameRunner.this, label, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
								return answer == JOptionPane.YES_OPTION;
							}
							
						};
						boolean success = oorr.createRelease();
						String message;
						if (success) {
							message = "Finished release manager process";
						}
						else {
							message = "Finished release manager process, but no release was created.";
						}
						logInfo(message);
						JOptionPane.showMessageDialog(ReleaseGuiMainFrameRunner.this, message);
					} catch (OboOntologyReleaseRunnerCheckException e) {
						String message = "Stopped Release process. Reason: \n"+e.renderMessageString();
						logError(message, e);
						JOptionPane.showMessageDialog(ReleaseGuiMainFrameRunner.this, createMultiLineLabel(message), "OORT Release Problem", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e) {
						String message = "Internal error: \n"+ e.getMessage();
						logError(message, e);
						JOptionPane.showMessageDialog(ReleaseGuiMainFrameRunner.this, createMultiLineLabel(message), "Error", JOptionPane.ERROR_MESSAGE);
					} catch (Throwable e) {
						String message = "Internal error: \n"+ e.getMessage();
						logError(message, e);
						JOptionPane.showMessageDialog(ReleaseGuiMainFrameRunner.this, createMultiLineLabel(message), "FatalError", JOptionPane.ERROR_MESSAGE);
					}
					finally {
						enableReleaseButton();
					}
				}
			};
			t.start();
		}
		
		protected void logInfo(String msg) {
			for (LogHandler handler : handlers) {
				handler.logInfo(msg);
			}
		}
		
		protected void logError(String msg, Throwable e) {
			for (LogHandler handler : handlers) {
				handler.logError(msg, e);
			}
		}
	}
	
	
	public static JComponent createMultiLineLabel(String s) {
		return new JLabel(convertToMultiline(s));
	}
	
	public static String convertToMultiline(String orig)
	{
	    return "<html><body style='width: 450px; padding: 5px;'><p>" + orig.replaceAll("\n", "<br>")+"</p></body></html>";
	}

}

package owltools.ontologyrelease;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JOptionPane;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

import owltools.ontologyrelease.gui.OboOntologyReleaseRunnerParameters;
import owltools.ontologyrelease.gui.ReleaseGuiMainFrame;

/**
 * GUI access for ontology release runner.
 */
public class OboOntologyReleaseRunnerGui {

	private final static Logger logger = Logger.getLogger(OboOntologyReleaseRunnerGui.class);
	
	// SimpleDateFormat is NOT thread safe
	// encapsulate as thread local
	private final static ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>(){

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};
	
	public static void main(String[] args) {
		
		Logger rootLogger = Logger.getRootLogger();
		final BlockingQueue<String> logQueue =  new ArrayBlockingQueue<String>(100);
		
		rootLogger.addAppender(new AppenderSkeleton() {
			
			public boolean requiresLayout() {
				return false;
			}
			
			public void close() {
				// do nothing
			}
			
			@Override
			protected void append(LoggingEvent event) {
				try {
					StringBuilder sb = new StringBuilder();
					sb.append(df.get().format(new Date(event.timeStamp)));
					sb.append(' ');
					sb.append(event.getLevel());
					sb.append(' ');
					sb.append(event.getRenderedMessage());
					logQueue.put(sb.toString());
				} catch (InterruptedException e) {
					logger.fatal("Interruped during wait for writing to the message panel.", e);
				}
			}
		});
		
		// Start GUI
		new ReleaseGuiMainFrameRunner(logQueue);
	}

	private static final class ReleaseGuiMainFrameRunner extends ReleaseGuiMainFrame {
		
		// generated
		private static final long serialVersionUID = -8690322825608397262L;
		
		private ReleaseGuiMainFrameRunner(BlockingQueue<String> logQueue) {
			super(logQueue);
		}
	
		@Override
		protected void executeRelease(final OboOntologyReleaseRunnerParameters parameters) {
			logger.info("Starting release manager process");
			disableReleaseButton();
			// execute the release in a separate Thread, otherwise the GUI might be blocked.
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						OWLOntologyFormat format = parameters.getFormat();
						Vector<String> paths = parameters.getPaths();
						File base = parameters.getBase();
						OboOntologyReleaseRunner oorr = new OboOntologyReleaseRunner() {

							@Override
							protected boolean allowFileOverwrite(File file) throws IOException {
								String message = "The release manager tried to overwrite existing files. Do you want to allow this?";
								String title = "Allow file overwrite?";
								int answer = JOptionPane.showConfirmDialog(ReleaseGuiMainFrameRunner.this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
								boolean allowOverwrite = answer == JOptionPane.YES_OPTION;
								ReleaseGuiMainFrameRunner.this.getAdvancedPanel().setAllowOverwrite(allowOverwrite);
								this.allowFileOverWrite = allowOverwrite;
								return allowOverwrite;
							}
							
						};
						oorr.setReasonerName(parameters.getReasoner());
						oorr.setAsserted(parameters.isAsserted());
						oorr.setSimple(parameters.isSimple());
						oorr.setExpandXrefs(parameters.isExpandXrefs());
						oorr.setAllowFileOverWrite(parameters.isAllowOverwrite());
						oorr.createRelease(format, paths, base);
						logger.info("Finished release manager process");
						JOptionPane.showMessageDialog(ReleaseGuiMainFrameRunner.this, "Finished making the release.");
					} catch (Exception e) {
						String message = "Internal error: "+ e.getMessage();
						logger.error(message, e);
						JOptionPane.showMessageDialog(ReleaseGuiMainFrameRunner.this, message, "Error", JOptionPane.ERROR_MESSAGE);
					} catch (Throwable e) {
						String message = "Internal error: "+ e.getMessage();
						logger.fatal(message, e);
						JOptionPane.showMessageDialog(ReleaseGuiMainFrameRunner.this, message, "FatalError", JOptionPane.ERROR_MESSAGE);
					}
					finally {
						enableReleaseButton();
					}
				}
			};
			t.start();
		}
	}
}

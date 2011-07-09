package owltools.ontologyrelease;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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

	public static void main(String[] args) {
		
		// SimpleDateFormat is NOT thread safe
		// encapsulate as thread local
		final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>(){

			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			}
			
		};
		
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
				String message = event.getRenderedMessage();
				logQueue.add(df.get().format(new Date(event.timeStamp))+"  "+message);
			}
		});
		
		final Logger logger = Logger.getLogger(OboOntologyReleaseRunnerGui.class);

		// Start GUI
		new ReleaseGuiMainFrame(logQueue) {

			// generated
			private static final long serialVersionUID = -8690322825608397262L;

			@Override
			protected void executeRelease(OboOntologyReleaseRunnerParameters parameters) {
				logger.info("Starting release manager process");
				try {
					OWLOntologyFormat format = parameters.getFormat();
					String reasoner = parameters.getReasoner();
					boolean asserted = parameters.isAsserted();
					boolean simple = parameters.isSimple();
					Vector<String> paths = parameters.getPaths();
					File base = parameters.getBase();
					OboOntologyReleaseRunner.createRelease(format, reasoner, asserted, simple, paths, base);
				} catch (Exception e) {
					logger.error("Internal error: "+ e.getMessage(), e);
				} catch (Throwable e) {
					logger.fatal("Internal error: "+ e.getMessage(), e);
				}
			}
		};
	}
}

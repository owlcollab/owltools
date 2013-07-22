package org.geneontology.annotation.view;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geneontology.lego.dot.LegoDotWriter;
import org.geneontology.lego.dot.LegoRenderer;
import org.geneontology.lego.model.LegoTools.UnExpectedStructureException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

/**
 * Tools for rendering LEGO annotations using Graphviz.
 */
public class GraphvizImageRenderer {

	private GraphvizImageRenderer() {
		super();
		// private, use only the static method
	}
	
	/**
	 * Exception indicating a problem with current Graphviz configuration.
	 */
	public static class GraphvizConfigurationError extends Exception {

		// generated
		private static final long serialVersionUID = -7655896872937780705L;

		/**
		 * @param message
		 */
		public GraphvizConfigurationError(String message) {
			super(message);
		}
	}
	
	/**
	 * Render all individuals as LEGO annotations using the Graphviz dot application.
	 * 
	 * @param graph
	 * @param reasoner
	 * @return image
	 * @throws IOException
	 * @throws UnExpectedStructureException thrown in case of unexpected axioms for LEGO annotations
	 * @throws InterruptedException
	 * @throws GraphvizConfigurationError thrown in case of detectable problems with graphiz and dot application configuration
	 */
	public static BufferedImage renderLegoAnnotations(OWLGraphWrapper graph, OWLReasoner reasoner) 
			throws IOException, UnExpectedStructureException, 
					InterruptedException, GraphvizConfigurationError 
	{
		// Step 0: check that graphviz/dot is configured
		String dotPath = LegoAnnotationsPreferences.getInstance().getDotPath();
		if (dotPath == null || dotPath.length() <= 3) {
			// no path configured, warn the user
			throw new GraphvizConfigurationError("No path configured for graphviz and the required 'dot' application");
		}
		File graphviz = new File(dotPath);
		if (!graphviz.exists()) {
			throw new GraphvizConfigurationError("The configured path does not exist: "+dotPath);
		}
		if (!graphviz.isFile()) {
			throw new GraphvizConfigurationError("The configured path is not a file: "+dotPath);
		}
		if (!graphviz.canExecute()) {
			throw new GraphvizConfigurationError("The configured file is not executable: "+dotPath);
		}
		
		final File dotFile = File.createTempFile("LegoAnnotations", ".dot");
		final File pngFile = File.createTempFile("LegoAnnotations", ".png");
		
		try {
			// Step 1: render dot file
			LegoRenderer dotWriter = new LegoDotWriter(graph, reasoner) {
				
				private PrintWriter writer = null;
				
				@Override
				protected void open() throws IOException {
					writer = new PrintWriter(dotFile);
				}
				
				@Override
				protected void appendLine(CharSequence line) throws IOException {
					writer.println(line);
				}

				@Override
				protected void close() {
					IOUtils.closeQuietly(writer);
				}
				
			};
			Set<OWLNamedIndividual> individuals = new HashSet<OWLNamedIndividual>();
			for(OWLOntology ont : graph.getAllOntologies()) {
				individuals.addAll(ont.getIndividualsInSignature());
			}
			dotWriter.render(individuals, null, true);
			
			// Step 2: render png file using graphiz (i.e. dot)
			Runtime r = Runtime.getRuntime();

			final String in = dotFile.getAbsolutePath();
			final String out = pngFile.getAbsolutePath();
			
			Process process = r.exec(dotPath + " " + in + " -Tpng -q -o " + out);

			process.waitFor();
			
			// Step 3: load resulting image into memory
			BufferedImage image = ImageIO.read(pngFile);
			return image;
		} finally {
			// delete temp files, do not rely on deleteOnExit
			FileUtils.deleteQuietly(dotFile);
			FileUtils.deleteQuietly(pngFile);
		}
	}
}

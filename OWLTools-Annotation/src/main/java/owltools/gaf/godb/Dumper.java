package owltools.gaf.godb;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.gaf.GafDocument;
import owltools.graph.OWLGraphWrapper;

/**
 * Generates a dump of a combined set of ontologies, annotations and related data
 * 
 * Currently there is one implementing subclass, for dumping a relational database.
 * 
 * In principle this could be extended to other dumps
 * 
 * @author cjm
 *
 */
public abstract class Dumper {

	private static Logger LOG = Logger.getLogger(Dumper.class);

	protected OWLGraphWrapper graph;
	protected String targetDirectory = "target";

	protected Set<GafDocument> gafdocs = new HashSet<GafDocument>();
	protected List<String> problems = new ArrayList<String>();


	/**
	 * @return dirname
	 */
	public String getTargetDirectory() {
		return targetDirectory;
	}


	/**
	 * @param targetDirectory
	 */
	public void setTargetDirectory(String targetDirectory) {
		this.targetDirectory = targetDirectory;
	}

	/**
	 * @param gd
	 */
	public void addGafDocument(GafDocument gd) {
		gafdocs.add(gd);
	}
	
	

	/**
	 * @return set of Gafs loaded
	 */
	public Set<GafDocument> getGafdocs() {
		return gafdocs;
	}

	/**
	 * @param gafdocs
	 */
	public void setGafdocs(Set<GafDocument> gafdocs) {
		this.gafdocs = gafdocs;
	}
	
	/**
	 * dumps all data
	 * 
	 * @throws IOException 
	 * @throws ReferentialIntegrityException 
	 * 
	 */
	public abstract void dump() throws IOException, ReferentialIntegrityException;
	
	protected void addProblem(String m) {
		LOG.warn(m);
		problems.add(m);
	}
	
	public void reportProblems() {
		if (problems.size() == 0) {
			LOG.info("No problems encountered");
		}
		else {
			LOG.warn("#Problems = "+problems.size());
			for (String m : problems) {
				LOG.warn(m);
			}
		}
	}

}

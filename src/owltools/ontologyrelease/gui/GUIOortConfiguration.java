package owltools.ontologyrelease.gui;

import java.io.File;
import java.util.Vector;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

import owltools.ontologyrelease.OboOntologyReleaseRunner;
import owltools.ontologyrelease.OboOntologyReleaseRunner.OortConfiguration;

/**
 * Add all options and paramets for {@link OboOntologyReleaseRunner} 
 * in one configuration object.
 */
public class GUIOortConfiguration extends OortConfiguration {
	
	private OWLOntologyFormat format = new RDFXMLOntologyFormat();
	private Vector<String> paths;
	private File base = new File(".");

	public GUIOortConfiguration() {
		super();
		// set values which are different from CLI default setting
		setAsserted(true); 
		setSimple(true);
		setExpandXrefs(false);
	}
	
	/**
	 * @return the format
	 */
	public OWLOntologyFormat getFormat() {
		return format;
	}

	/**
	 * @param format the format to set
	 */
	public void setFormat(OWLOntologyFormat format) {
		this.format = format;
	}

	/**
	 * @return the paths
	 */
	public Vector<String> getPaths() {
		return paths;
	}

	/**
	 * @param paths the paths to set
	 */
	public void setPaths(Vector<String> paths) {
		this.paths = paths;
	}

	/**
	 * @return the base
	 */
	public File getBase() {
		return base;
	}

	/**
	 * @param base the base to set
	 */
	public void setBase(File base) {
		this.base = base;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OboOntologyReleaseRunnerParameters [");
		if (format != null) {
			builder.append("format=");
			builder.append(format.getClass().getSimpleName());
			builder.append(", ");
		}
		if (getReasonerName() != null) {
			builder.append("reasoner=");
			builder.append(getReasonerName());
			builder.append(", ");
		}
		builder.append("asserted=");
		builder.append(isAsserted());
		builder.append(", simple=");
		builder.append(isSimple());
		builder.append(", ");
		builder.append(", allowOverwrite=");
		builder.append(isAllowFileOverWrite());
		builder.append(", ");
		if (paths != null) {
			builder.append("paths=");
			builder.append(paths);
			builder.append(", ");
		}
		if (base != null) {
			builder.append("base=");
			builder.append(base);
		}
		builder.append("]");
		return builder.toString();
	}
}

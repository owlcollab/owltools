package owltools.ontologyrelease.gui;

import java.io.File;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import owltools.ontologyrelease.OboOntologyReleaseRunner;
import owltools.ontologyrelease.OboOntologyReleaseRunner.OortConfiguration;

/**
 * Add all options and parameters for {@link OboOntologyReleaseRunner} 
 * in one configuration object.
 */
public class GUIOortConfiguration extends OortConfiguration {
	
	private Vector<String> paths;
	private File base = new File(FileUtils.getUserDirectory(),"OORT");

	public GUIOortConfiguration() {
		super();
		// set values which are different from CLI default setting
		setAsserted(true); 
		setSimple(true);
		setExpandXrefs(false);
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
		builder.append("GUIOortConfiguration [");
		if (paths != null) {
			builder.append("paths=");
			builder.append(paths);
			builder.append(", ");
		}
		if (base != null) {
			builder.append("base=");
			builder.append(base);
			builder.append(", ");
		}
		if (getReasonerName() != null) {
			builder.append("reasonerName=");
			builder.append(getReasonerName());
			builder.append(", ");
		}
		builder.append("enforceEL=");
		builder.append(isEnforceEL());
		builder.append(", writeELOntology=");
		builder.append(isWriteELOntology());
		builder.append(", asserted=");
		builder.append(isAsserted());
		builder.append(", simple=");
		builder.append(isSimple());
		builder.append(", allowFileOverWrite=");
		builder.append(isAllowFileOverWrite());
		builder.append(", isExpandXrefs=");
		builder.append(isExpandXrefs());
		builder.append(", isRecreateMireot=");
		builder.append(isRecreateMireot());
		builder.append(", isExpandMacros=");
		builder.append(isExpandShortcutRelations());
		if (getMacroStrategy() != null) {
			builder.append(", ");
			builder.append("macroStrategy=");
			builder.append(getMacroStrategy());
		}
		builder.append(", executeOntologyChecks=");
		builder.append(isExecuteOntologyChecks());
		builder.append("]");
		return builder.toString();
	}
}

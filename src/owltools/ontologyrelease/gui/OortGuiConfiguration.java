package owltools.ontologyrelease.gui;

import java.io.File;

import org.apache.commons.io.FileUtils;

import owltools.ontologyrelease.OboOntologyReleaseRunnerGui;
import owltools.ontologyrelease.OortConfiguration;

/**
 * Parameters for {@link OboOntologyReleaseRunnerGui} with 
 * different default values.
 */
public class OortGuiConfiguration extends OortConfiguration {
	
	public OortGuiConfiguration() {
		super();
		// set values which are different from CLI default setting
		setAsserted(true); 
		setSimple(true);
		setExpandXrefs(false);
		setBase(new File(FileUtils.getUserDirectory(),"OORT"));
	}
}

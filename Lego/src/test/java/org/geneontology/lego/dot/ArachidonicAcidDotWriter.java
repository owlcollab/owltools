package org.geneontology.lego.dot;

import java.io.File;

/**
 * Create a dot file for the Dauer pathway example.
 */
public class ArachidonicAcidDotWriter {

	public static void main(String[] args) throws Exception {
		// create work dir
		File file = new File("out");
		file.mkdirs();
		// notes: should check for inconsistencies first
		ExampleWriter.write("src/test/resources/arachidonic-acid.owl", "out/arachidonic-acid.dot", "aa", null);
	}
}

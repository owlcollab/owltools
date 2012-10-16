package org.geneontology.lego.dot;

import java.io.File;

/**
 * Create a dot file for the Dauer pathway example.
 */
public class DauerExampleDotWriter {

	public static void main(String[] args) throws Exception {
		// create work dir
		File file = new File("out");
		file.mkdirs();
		ExampleWriter.write("src/test/resources/dauer-merged.owl", "out/dauer.dot", "dauer", null);
	}
}

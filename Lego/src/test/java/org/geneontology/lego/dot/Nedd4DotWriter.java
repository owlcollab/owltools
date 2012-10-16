package org.geneontology.lego.dot;

import java.io.File;


/**
 * Create a dot file for the NEDD4 example.
 */
public class Nedd4DotWriter {

	public static void main(String[] args) throws Exception {
		// create work dir
		File file = new File("out");
		file.mkdirs();
		ExampleWriter.write("src/test/resources/NEDD4-merged.owl", "out/NEDD4.dot", "NEDD4", null);
	}
}

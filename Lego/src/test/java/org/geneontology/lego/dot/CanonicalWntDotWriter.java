package org.geneontology.lego.dot;

import java.io.File;

public class CanonicalWntDotWriter {

	public static void main(String[] args) throws Exception {
		// create work dir
		File file = new File("out");
		file.mkdirs();
		ExampleWriter.write("src/test/resources/examples/canonical-wnt.owl", "out/wnt.dot", "canonical-wnt", "src/test/resources/examples/catalog-v001.xml");
	}
}

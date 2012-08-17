package org.geneontology.lego.dot;

import java.io.File;

public class Pmid21771813Writer {

	public static void main(String[] args) throws Exception {
		// create work dir
		File file = new File("out");
		file.mkdirs();
		DauerExampleDotWriter.write("examples/lr-asymmetry-pmid21771813.owl", "out/pmid21771813.dot", "lr asymmetry - PMID 21771813 ");
	}
}

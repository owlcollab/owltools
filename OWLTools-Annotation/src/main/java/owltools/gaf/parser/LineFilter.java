package owltools.gaf.parser;

public interface LineFilter {

	/**
	 * Given a line in a gene annotation file, decide whether to load the line.
	 * This can be used to create filters to exclude certain types of
	 * annotations, such as IEA.
	 * 
	 * @param line
	 * @param pos
	 * @param parser
	 * @return true, if the line should be loaded
	 */
	public boolean accept(String line, int pos, GAFParser parser);
}

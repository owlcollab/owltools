package owltools.flex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Pull defined sources into a middle state for output, loading into Solr, etc.
 * 
 * TODO: allow clobbering and non-clobbering versions of add.
 */
public class FlexDocument implements Iterable<FlexLine> {
	
	//private static Logger LOG = Logger.getLogger(FlexDocument.class);	
	protected List<FlexLine> lines = null;
	
	/**
	 * Init.
	 */
	public FlexDocument() {
		lines = new ArrayList<FlexLine>();
	}

	/**
	 * Add a line to the document.
	 * TODO: allow clobbering and non-clobbering versions of add.
	 *  
	 * @param line
	 */
	public void add(FlexLine line){
		lines.add(line);
	}
	
	@Override
	public Iterator<FlexLine> iterator() {
		return lines.iterator();
	}
}

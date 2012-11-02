package owltools.graph.shunt;

import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

/**
 * Iterate over an OWLShuntGraph.
 * 
 * See: {@link owltools.graph.shunt.OWLShuntGraph}.
 * 
 */
public class OWLShuntGraphDFIterator implements Iterator<String>{
	
	private Map<String,Boolean> color = new HashMap<String,Boolean>();
	private List<String> todo = new ArrayList<String>();
	private List<String> done = new ArrayList<String>();
	private Iterator<String> iter;
	
	/**
	 * Constructor.
	 * 
	 * @param g
	 * @return this
	 */
	public OWLShuntGraphDFIterator(OWLShuntGraph g) {	
		
		// Get the roots, and push them (unordered)
		// onto the order list. Color them.
		Set<String> roots = g.getRoots();
		for( String r : roots ){
			todo.add(r);
			//System.err.println("i root: " + r);
		}
		
		// Cycle through the todo list, coloring and collecting.
		while( ! todo.isEmpty() ){
			
			// Grab the next todo element.
			String next = todo.remove(0);
			//System.err.println("i next: " + next);
			
			// Only operate on it if we haven't seen it before.
			if( ! color.containsKey(next) ){
				color.put(next, true); // make sure we don't see it again

				// Add the children to the todo if there are any.
				Set<String> kids = g.getChildren(next);
				for( String k : kids ){
					todo.add(0, k);
					//System.err.println("i add kid: " + k);
				}				
				
				// Add self to the finished DFS ordered list.
				done.add(next);
				//System.err.println("i add final: " + next);
			}
		}
		
		// Transfer all of the iterator operations to this Iterator proxy...
		iter = done.iterator();
	}

	@Override
	public boolean hasNext() {
		return iter.hasNext();
	}

	@Override
	public String next() {
		return iter.next();
	}

	@Override
	public void remove() {
		iter.remove();
	}
}

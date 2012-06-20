package owltools.flex;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * Workalike for a singleton line in a Solr document.
 */
public class FlexLine implements Iterable<String> {
	
	//private static Logger LOG = Logger.getLogger(FlexLine.class);
	protected String field = null;
	protected ArrayList<String> value = null;
		
	/**
	 * Singleton init.
	 * 
	 * @param field
	 * @param value
	 */
	public FlexLine(String in_field, String in_value) {

		field = in_field;
		value = new ArrayList<String>();
		value.add(in_value);
	}

	/**
	 * List init.
	 * 
	 * @param field
	 * @param value
	 */	
	public FlexLine(String in_field, ArrayList<String> in_values) {

		field = in_field;
		value = new ArrayList<String>();
		for( String in_value : in_values ){
			value.add(in_value);
		}
	}
	
	/**
	 * Return the field name.
	 * 
	 * @return
	 */
	public String field (){
		return field;
	}
	
	/**
	 * Return all the values.
	 * 
	 * @return
	 */
	public ArrayList<String> values (){
		return value;
	}
	
	/**
	 * Return whether or not the line has a single value or multiple values.
	 * 
	 * @return
	 */
	public boolean isSingle (){
		boolean retval = true;
		if( value.size() != 1 ){
			retval = false;
		}
		return retval;
	}

	/**
	 * Return whether or not the line has a single value or multiple values.
	 * 
	 * @return
	 */
	public boolean isMulti (){
		return ! isSingle();
	}
	
	/**
	 * Return the first/only value.
	 * 
	 * @return
	 */
	public String value (){
		return value.get(0);
	}

	@Override
	public Iterator<String> iterator() {
		return value.iterator();
	}
}

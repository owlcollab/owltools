package owltools.flex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Workalike for a singleton line in a Solr document.
 */
public class FlexLine implements Iterable<String> {
	
	protected String field = null;
	protected List<String> value = null;
		
	/**
	 * Singleton init.
	 * 
	 * @param in_field
	 * @param in_value
	 */
	public FlexLine(String in_field, String in_value) {

		field = in_field;
		value = new ArrayList<String>();
		value.add(in_value);
	}

	/**
	 * List init.
	 * 
	 * @param in_field
	 * @param in_values
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
	 * @return field name
	 */
	public String field (){
		return field;
	}
	
	/**
	 * Return all the values.
	 * 
	 * @return values
	 */
	public List<String> values (){
		return value;
	}
	
	/**
	 * Return whether or not the line has a single value or multiple values.
	 * 
	 * @return boolean
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
	 * @return boolean
	 */
	public boolean isMulti (){
		return ! isSingle();
	}
	
	/**
	 * Return the first/only value.
	 * 
	 * @return boolean
	 */
	public String value (){
		return value.get(0);
	}

	@Override
	public Iterator<String> iterator() {
		return value.iterator();
	}
}

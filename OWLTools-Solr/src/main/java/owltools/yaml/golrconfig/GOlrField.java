package owltools.yaml.golrconfig;

import java.util.ArrayList;

public class GOlrField {

	public String id;
	public String description;
	public String display_name;
	public String type;
	public String required;
	public String cardinality;
	public String property;
	public String property_type;
	public int weight;	
	// The processing steps to apply to that property--not yet used
	public ArrayList<String> transform;

	// Define the defaults for optional fields.
	public GOlrField() {
		required = "false";
		cardinality = "single";
		property_type = "dynamic";
		weight = 0;
		
		// There are no default transformations to make.
		transform = new ArrayList<String>();
	}
}

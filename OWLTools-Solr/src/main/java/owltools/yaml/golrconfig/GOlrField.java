package owltools.yaml.golrconfig;

import java.util.ArrayList;

public class GOlrField {

	// NOTE: required and searchable are slightly fudged here--they are bool as far as the YAML goes,
	// but they kind of end up strings at the Solr end of things as that's mostly how they go
	// in between.
	public String id;
	public String description;
	public String display_name;
	public String type;
	public String required;
	public String cardinality;
	public String property;
	public String property_type;
	public int weight;	
	public String searchable;
	// The processing steps to apply to that property--not yet used
	public ArrayList<String> transform;

	// Define the defaults for optional fields.
	public GOlrField() {
		required = "false";
		cardinality = "single";
		property_type = "dynamic";
		weight = 0;
		searchable = "false";
		
		// There are no default transformations to make.
		transform = new ArrayList<String>();
	}
}

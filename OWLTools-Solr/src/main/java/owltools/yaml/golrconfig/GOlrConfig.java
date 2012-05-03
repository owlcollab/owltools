package owltools.yaml.golrconfig;

import java.util.ArrayList;

public class GOlrConfig {

	public String id;
	public String description;
	public String display_name;
	public String document_category;
	public int weight;
	public String searchable_extension;
	public String default_fields_and_boosts;

//	public ArrayList<GOlrFixedField> fixed;
//	public ArrayList<GOlrDynamicField> dynamic;
	public ArrayList<GOlrField> fields;
	
	// Define the defaults for optional fields.
	public GOlrConfig() {
		searchable_extension = "_searchable";
		weight = 0;
		default_fields_and_boosts = "";
	}
}


package owltools.yaml.golrconfig;

import java.util.ArrayList;

public class GOlrConfig {

	public String id;
	public String description;
	public String display_name;
	public int weight;

	public ArrayList<GOlrFixedField> fixed;
	public ArrayList<GOlrDynamicField> dynamic;
	
	// Define the defaults for optional fields.
	public GOlrConfig() {
		weight = 0;
	}
}


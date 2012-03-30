package owltools.yaml.golrconfig;

public class GOlrCoreField {

	public String id;
	public String description;
	public String display_name;
	public String type;
	public String required;
	public String cardinality;

	public GOlrCoreField() {
		//required = "false";
		cardinality = "single";
	}
}

package owltools.yaml.golrconfig;

import java.util.ArrayList;

@Deprecated
public class GOlrDynamicField extends GOlrField{

	public ArrayList<String> transform; // The processing steps to apply to that property.

	// Define the defaults for optional fields.
	public GOlrDynamicField (){
		transform = new ArrayList<String>();
	}
}

package owltools.yaml.golrconfig;

import java.util.ArrayList;

public class GOlrDynamicField extends GOlrCoreField{

	public String property; // The place to look for the value that will go here.
	public ArrayList<String> transform; // The processing steps to apply to that property.
	
	public GOlrDynamicField (){
		transform = new ArrayList<String>();
	}
	
}

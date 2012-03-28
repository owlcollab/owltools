package owltools.yaml.flexconfig;

import java.util.ArrayList;

public class FlexDocDynamicField extends FlexDocCore{

	public String type;
	public String property;
	public ArrayList<String> transform;
	
	public FlexDocDynamicField (){
		transform = new ArrayList<String>();
	}
	
}

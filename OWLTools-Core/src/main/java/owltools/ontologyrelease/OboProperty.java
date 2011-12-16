package owltools.ontologyrelease;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.taskdefs.Property;

@Deprecated
public class OboProperty extends Property {

	public OboProperty() {
		super();
	}

	public OboProperty(boolean userProperty, Project fallback) {
		super(userProperty, fallback);
	}

	public OboProperty(boolean userProperty) {
		super(userProperty);
	}

	@Override
	public void execute() throws BuildException {
		super.execute();

		PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(getProject());
	
		
		Object asserted = propertyHelper.getProperty("oborelease.asserted");
		
		if(asserted != null && asserted.toString().equals("yes")){
			addProperty("oborelease.asserted.option", "--asserted");
		}else{
			addProperty("oborelease.asserted.option", "");
		}
		
		Object simple = propertyHelper.getProperty("oborelease.simple");
		
		if(simple != null && "yes".equals(simple.toString())){
			addProperty("oborelease.simple.option", "--simple");
		}else{
			addProperty("oborelease.simple.option", "");
		}
	
		/*Object oboIncludes = propertyHelper.getProperty("oborelease.oboincludes");
		
		if(oboIncludes != null && oboIncludes.toString().trim().length()>0){
			addProperty("oborelease.oboincludes.option", "-oboincludes " + oboIncludes);
		}else{
			addProperty("oborelease.oboincludes.option", "" + oboIncludes);
		}*/
		
		
	}
	
}

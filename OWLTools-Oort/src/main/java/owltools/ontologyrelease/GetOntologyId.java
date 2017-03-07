package owltools.ontologyrelease;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

@Deprecated
public class GetOntologyId extends Task {

	
	private String ontologyLocation;

	public void setFile(String file){
		if(file != null){
			String files[] = file.split(" ");
			this.ontologyLocation = files[0];
		}
	}
	
	public String getFile(){
		return this.ontologyLocation;
	}
	
	private OWLOntology getOntology() throws Exception{
		if(ontologyLocation.endsWith(".owl")){
			//oborelease.isOWLAPIObo2Owl			
			addProperty("oborelease.isowl", "true");
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			return manager.loadOntologyFromOntologyDocument(new File(ontologyLocation));
		}
	
		addProperty("oborelease.isobo", "true");
		
		OWLAPIObo2Owl obo2owl = new  OWLAPIObo2Owl(OWLManager.createOWLOntologyManager() );
		
		return obo2owl.convert(ontologyLocation);
	}
	
	public void execute(){

        if (null == ontologyLocation) {
        	throw new BuildException("Ontology File location is not set via file attribute");
        }
		
 		Project project = getProject();
		
		if(project == null){
            throw new IllegalStateException("project has not been set");
		}
		
		try{
			OWLOntology ontology = getOntology();
			
			String ontologyId = OWLAPIOwl2Obo.getOntologyId(ontology);
			
	        /*PropertyHelper propertyHelper
	        = (PropertyHelper) PropertyHelper.getPropertyHelper(getProject());
			
	        
	        propertyHelper.setNewProperty("oborelease.ontologyid", ontologyId);*/
			
			addProperty("oborelease.ontologyid", ontologyId);
		}catch(Exception ex){
			throw new BuildException(ex);
		}
	}
	
//	   /**
//     * Validate that the task parameters are valid.
//     *
//     * @throws BuildException if parameters are invalid
//     */
  /*  private void validate()
         throws BuildException {
        if (null == ontologyLocation) {
        	throw new BuildException("Ontology File location is not set via file attribute");
        }
        
        if(!ontologyLocation.exists()){
        	throw new BuildException("Ontology File location is not set via file attribute");
        }
        if (!ontologyLocation.canRead()) {
            final String message = "Unable to read from " + ontologyLocation + ".";
            throw new BuildException(message);
        }

    }*/
	
    private void addProperty(String n, Object v) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(getProject());
         ph.setNewProperty(n, v);
    }
	
	
}

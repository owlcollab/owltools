package owltools.sim.preprocessor;

public class GOSimPreProcessor extends AbstractOBOSimPreProcessor {
	
	public void preprocess() {
		makePartOfReflexive();
		
		//this.createPropertyView(this.oboPartOf(), rootClass, labelFormat)
		
	
	}

}

package owltools.gaf.lego.json;


public class JsonOwlIndividual extends JsonAnnotatedObject {
	public String id;
	public String label; //  TODO why do we have this? an individual should never have a label, right?
	public JsonOwlObject[] type;
}
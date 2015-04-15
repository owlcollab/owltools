package owltools.gaf.lego.json;

import java.util.Arrays;


public class JsonOwlIndividual extends JsonAnnotatedObject {
	public String id;
	public String label; //  TODO why do we have this? an individual should never have a label, right?
	public JsonOwlObject[] type;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + Arrays.hashCode(type);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JsonOwlIndividual other = (JsonOwlIndividual) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (label == null) {
			if (other.label != null) {
				return false;
			}
		} else if (!label.equals(other.label)) {
			return false;
		}
		if (!Arrays.equals(type, other.type)) {
			return false;
		}
		return true;
	}
}
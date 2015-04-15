package owltools.gaf.lego.json;

import java.util.Arrays;

public class JsonModel extends JsonAnnotatedObject {
	public JsonOwlIndividual[] individuals;
	public JsonOwlFact[] facts;
	public JsonOwlObject[] properties;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(facts);
		result = prime * result + Arrays.hashCode(individuals);
		result = prime * result + Arrays.hashCode(properties);
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
		JsonModel other = (JsonModel) obj;
		if (!Arrays.equals(facts, other.facts)) {
			return false;
		}
		if (!Arrays.equals(individuals, other.individuals)) {
			return false;
		}
		if (!Arrays.equals(properties, other.properties)) {
			return false;
		}
		return true;
	}
}
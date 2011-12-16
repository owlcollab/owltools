package owltools.phenolog;

import java.util.HashSet;
import java.util.Set;

public class PhenologEngine {

	Set<IndividualPair> individualPairs = new HashSet<IndividualPair>();

	public Set<IndividualPair> getIndividualPairs() {
		return individualPairs;
	}

	public void setIndividualPairs(Set<IndividualPair> individualPairs) {
		this.individualPairs = individualPairs;
	}

	private float calculateSimilarity(Attribute a1, Attribute a2) {
		return 0; // TODO
	}

}

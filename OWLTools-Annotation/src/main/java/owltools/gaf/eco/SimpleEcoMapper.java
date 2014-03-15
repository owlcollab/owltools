package owltools.gaf.eco;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

public interface SimpleEcoMapper {

	public String getEco(String goCode, String ref);
	
	public String getEco(String goCode, Collection<String> allRefs);
	
	public Pair<String, String> getGoCode(String eco);
}

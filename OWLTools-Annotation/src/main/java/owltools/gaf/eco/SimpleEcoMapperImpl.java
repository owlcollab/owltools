package owltools.gaf.eco;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import owltools.gaf.eco.EcoMapperFactory.EcoMappings;

public class SimpleEcoMapperImpl implements SimpleEcoMapper {
	
	private final EcoMapperFactory.EcoMappings<String> mappings;
	private final Map<String, Pair<String, String>> reverseMap;

	SimpleEcoMapperImpl(EcoMappings<String> mappings) {
		this.mappings = mappings;
		reverseMap = mappings.getReverseMap();
	}

	@Override
	public String getEco(String goCode, String ref) {
		return mappings.get(goCode, ref);
	}

	@Override
	public String getEco(String goCode, Collection<String> allRefs) {
		String eco = null;
		for (String ref : allRefs) {
			eco = mappings.get(goCode, ref);
			if (eco != null) {
				break;
			}
		}
		return eco;
	}

	@Override
	public Pair<String, String> getGoCode(String eco) {
		return reverseMap.get(eco);
	}

}

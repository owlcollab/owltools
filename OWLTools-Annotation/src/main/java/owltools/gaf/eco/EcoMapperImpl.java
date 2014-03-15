package owltools.gaf.eco;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.OWLClass;

public class EcoMapperImpl implements EcoMapper {
	
	private final EcoMapperFactory.EcoMappings<OWLClass> mappings;
	
	EcoMapperImpl(EcoMapperFactory.EcoMappings<OWLClass> mappings) {
		this.mappings = mappings;
	}

	@Override
	public OWLClass getEcoClassForCode(String code) {
		return mappings.get(code);
	}

	@Override
	public Set<OWLClass> getAllEcoClassesForCode(String code) {
		return mappings.getAll(code);
	}

	@Override
	public OWLClass getEcoClassForCode(String code, String refCode) {
		return mappings.get(code, refCode);
	}

	@Override
	public boolean isGoEvidenceCode(String code) {
		return mappings.hasCode(code);
	}

	@Override
	public Map<OWLClass, String> getCodesForEcoClasses() {
		Map<OWLClass, Pair<String, String>> fullReverseMap = mappings.getReverseMap();
		Map<OWLClass, String> simpleReverseMap = new HashMap<OWLClass, String>();
		for(Entry<OWLClass, Pair<String, String>> e : fullReverseMap.entrySet()) {
			String ref = e.getValue().getRight();
			if (ref == null) {
				simpleReverseMap.put(e.getKey(), e.getValue().getLeft());
			}
			
		}
		return simpleReverseMap;
	}

}

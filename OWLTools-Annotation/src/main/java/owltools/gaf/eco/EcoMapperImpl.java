package owltools.gaf.eco;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

public class EcoMapperImpl implements EcoMapper {
	
	private final EcoMapperFactory.EcoMappings mappings;
	
	EcoMapperImpl(EcoMapperFactory.EcoMappings mappings) {
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

}

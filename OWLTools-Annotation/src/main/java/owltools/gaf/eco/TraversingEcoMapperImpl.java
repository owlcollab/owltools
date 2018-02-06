package owltools.gaf.eco;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.eco.EcoMapperFactory.EcoMappings;

public class TraversingEcoMapperImpl extends EcoMapperImpl implements TraversingEcoMapper {

	private final OWLReasoner reasoner;
	private final boolean disposeReasoner;
	
	private final Map<String, Set<OWLClass>> mappingCache = new HashMap<String, Set<OWLClass>>();
	
	TraversingEcoMapperImpl(EcoMappings mappings, OWLReasoner reasoner,  boolean disposeReasoner) {
		super(mappings);
		this.reasoner = reasoner;
		this.disposeReasoner = disposeReasoner;
	}

	@Override
	public Set<OWLClass> getAncestors(Set<OWLClass> sources, boolean reflexive) {
		if (sources == null || sources.isEmpty()) {
			return Collections.emptySet();
		}
		Set<OWLClass> result = new HashSet<OWLClass>();
		for (OWLClass source : sources) {
			Set<OWLClass> set = reasoner.getSuperClasses(source, false).getFlattened();
			for (OWLClass cls : set) {
				if (cls.isBuiltIn() == false) {
					result.add(cls);
				}
			}
		}
		if (reflexive) {
			result.addAll(sources);
		}
		if (result.isEmpty()) {
			return Collections.emptySet();
		}
		return result;
	}

	@Override
	public Set<OWLClass> getAncestors(OWLClass source, boolean reflexive) {
		return getAncestors(Collections.singleton(source), reflexive);
	}

	@Override
	public Set<OWLClass> getDescendents(Set<OWLClass> sources, boolean reflexive) {
		if (sources == null || sources.isEmpty()) {
			return Collections.emptySet();
		}
		Set<OWLClass> result = new HashSet<OWLClass>();
		for (OWLClass source : sources) {
			Set<OWLClass> set = reasoner.getSubClasses(source, false).getFlattened();
			for (OWLClass cls : set) {
				if (cls.isBuiltIn() == false) {
					result.add(cls);
				}
			}
		}
		if (reflexive) {
			result.addAll(sources);
		}
		if (result.isEmpty()) {
			return Collections.emptySet();
		}
		return result;
	}

	@Override
	public Set<OWLClass> getDescendents(OWLClass source, boolean reflexive) {
		return getDescendents(Collections.singleton(source), reflexive);
	}

	@Override
	public Set<String> getAllValidEvidenceIds(String code, boolean includeChildren) {
		return getAllValidEvidenceIds(Collections.singleton(code), includeChildren);
	}

	@Override
	public Set<String> getAllValidEvidenceIds(Set<String> codes, boolean includeChildren) {
		if (codes == null || codes.isEmpty()) {
			return Collections.emptySet();
		}
		Set<String> result = new HashSet<String>();
		for(String code : codes) {
			Set<OWLClass> classes = getAllEcoClassesForCode(code);
			for (OWLClass owlClass : classes) {
				result.add(getId(owlClass));
			}
			if (includeChildren) {
				Set<OWLClass> descendents = getDescendents(classes, false);
				for (OWLClass owlClass : descendents) {
					result.add(getId(owlClass));
				}
			}
		}
		result.addAll(codes);
		return result;
	}
	
	private String getId(OWLClass cls) {
		return OWLAPIOwl2Obo.getIdentifier(cls.getIRI());
	}

	@Override
	public void dispose() {
		mappingCache.clear();
		if (disposeReasoner) {
			reasoner.dispose();
		}
	}
	
}

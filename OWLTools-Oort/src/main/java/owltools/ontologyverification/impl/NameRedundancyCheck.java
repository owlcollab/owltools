package owltools.ontologyverification.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.ontologyverification.CheckWarning;

/**
 * Check for redundant names in labels and synonyms (scope EXACT).<br>
 * By default all obsoleted objects are ignored.
 */
public class NameRedundancyCheck extends AbstractCheck {

	public static final String SHORT_HAND = "name-redundancy";
	
	private boolean ignoreObsolete = true;
	
	public NameRedundancyCheck() {
		super("NAME_REDUNDANCY_CHECK", "Name Redundancy Check", false, null);
	}
	
	/**
	 * @return the ignoreObsolete
	 */
	public boolean isIgnoreObsolete() {
		return ignoreObsolete;
	}

	/**
	 * @param ignoreObsolete the ignoreObsolete to set
	 */
	public void setIgnoreObsolete(boolean ignoreObsolete) {
		this.ignoreObsolete = ignoreObsolete;
	}

	@Override
	public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {

            List<CheckWarning> out = new ArrayList<CheckWarning>();
            
            // create index for all classes get label and exact synonyms
            // global
            Map<String, Set<OWLEntity>> labels = new HashMap<String, Set<OWLEntity>>();
            Map<String, Set<OWLEntity>> synonyms = new HashMap<String, Set<OWLEntity>>();
            // local; check for duplicate synonym labels for one term
			Set<String> localSynoyms = new HashSet<String>();
			
            for (OWLObject owlObject : allOwlObjects) {
            	if (owlObject instanceof OWLEntity == false) {
					continue;
				}
            	if (ignoreObsolete && graph.isObsolete(owlObject)) {
            		continue;
            	}
            	
            	OWLEntity owlEntity = (OWLEntity) owlObject;
            	final IRI iri = owlEntity.getIRI();
				final String label = graph.getLabel(owlObject);
				
				if (label == null) {
					out.add(new CheckWarning("HAS_NAME_CHECK", "The term with IRI: "+iri.toQuotedString()+" has no label.", isFatal(), iri));
					continue;
				}
				
				addValue(label, owlEntity, labels);
				
				List<ISynonym> oboSynonyms = graph.getOBOSynonyms(owlObject);
				if (oboSynonyms != null && !oboSynonyms.isEmpty()) {
					localSynoyms.clear();
					for (ISynonym synonym : oboSynonyms) {
						final String synonymLabel = synonym.getLabel();
						if (OboFormatTag.TAG_EXACT.getTag().equals(synonym.getScope())) {
							// add exact synonyms to global index
							addValue(synonymLabel, owlEntity, synonyms);
						}
						
						// local synonym check
						if (localSynoyms.add(synonymLabel) == false) {
							// there is a synonym with the same label already
							String message = "Duplicate synonym '"+synonymLabel+"' label for IRI: "+iri+" '"+label+"'";
							out.add(new CheckWarning(getID(), message , isFatal(), iri, OboFormatTag.TAG_SYNONYM.getTag()));
						}
 					}
				}
			}
            
            // check for conflicts in the index
            for(Entry<String, Set<OWLEntity>> entry : labels.entrySet()) {
            	String label = entry.getKey();
            	Set<OWLEntity> entities = entry.getValue();
            	if (entities.size() > 1) {
					// multiple entities with the same primary label
            		StringBuilder sb = new StringBuilder("Duplicate label '");
            		sb.append(label).append("' for IRIs: ");
            		List<IRI> iris = renderEntities(entities, sb, graph);
					out.add(new CheckWarning(getID(), sb.toString(), isFatal(), iris, OboFormatTag.TAG_NAME.getTag()));
				}
            	Set<OWLEntity> conflictingSynonyms = synonyms.get(label);
            	if (conflictingSynonyms != null) {
            		// remove entities which have the same primary label
            		Set<OWLEntity> cleaned = new HashSet<OWLEntity>();
            		for (OWLEntity owlEntity : conflictingSynonyms) {
						if (!entities.contains(owlEntity)) {
							cleaned.add(owlEntity);
						}
					}
            		if (!cleaned.isEmpty()) {
						// entities with synonyms, which are supposed to be unique labels
	            		IRI mainIRI = entities.iterator().next().getIRI();
	            		List<IRI> iris = new ArrayList<IRI>(cleaned.size() + 1);
	            		iris.add(mainIRI);
	            		StringBuilder sb = new StringBuilder("Primary label '");
	            		sb.append(label).append("' ").append(mainIRI.toQuotedString());
	            		sb.append(" re-used as EXACT synonym for IRIs: ");
	            		iris = renderEntities(cleaned, sb, graph, iris);
	            		out.add(new CheckWarning(getID(), sb.toString(), isFatal(), iris, OboFormatTag.TAG_SYNONYM.getTag()));
            		}
				}
            }
            
            return out;
        }

	private <K, V> void addValue(K key, V value, Map<K, Set<V>> map) {
		if (key != null) {
			Set<V> set = map.get(key);
			if (set == null) {
				map.put(key, Collections.singleton(value));
			}
			else if (set.size() == 1) {
				if (!set.contains(value)) {
					set = new HashSet<V>(set);
					set.add(value);
					map.put(key, set);
				}
			}
			else {
				set.add(value);
			}
		}
	}
	
	private List<IRI> renderEntities(Collection<? extends OWLEntity> entities, StringBuilder sb, OWLGraphWrapper g) {
		List<IRI> iris = new ArrayList<IRI>(entities.size());
		return renderEntities(entities, sb, g, iris);
	}
	
	private List<IRI> renderEntities(Collection<? extends OWLEntity> entities, StringBuilder sb, OWLGraphWrapper g, List<IRI> iris) {
		boolean first = true;
		for(OWLEntity entity : entities) {
			String entityLabel = g.getLabel(entity);
			IRI iri = entity.getIRI();
			iris.add(iri);
			if (first) {
				first = false;
			}
			else {
				sb.append("; ");
			}
			sb.append(iri.toQuotedString());
			if (entityLabel != null) {
				sb.append(" '").append(entityLabel).append("'");
			}
		}
		return iris;
	}

}

package owltools.annotation.lego.generate;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bbop.golr.java.RetrieveGolrAnnotations;
import org.bbop.golr.java.RetrieveGolrAnnotations.GolrAnnotationDocument;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

public class GolrSeedingDataProvider implements SeedingDataProvider {
	
	private final RetrieveGolrAnnotations golr;
	private final OWLGraphWrapper graph;
	private final OWLReasoner reasoner;
	private final Set<OWLClass> locationRoots;
	private Set<String> evidenceRestriction = null;
	private Set<String> taxonRestriction;
	private final Set<String> blackList;
	
	public GolrSeedingDataProvider(String golrServer, OWLGraphWrapper graph, OWLReasoner reasoner, Set<OWLClass> locationRoots, Set<String> evidenceRestriction, Set<String> taxonRestriction, Set<String> blackList) {
		this.graph = graph;
		this.reasoner = reasoner;
		this.evidenceRestriction = evidenceRestriction;
		this.locationRoots = locationRoots;
		this.taxonRestriction = taxonRestriction;
		this.blackList = blackList;
		golr = new RetrieveGolrAnnotations(golrServer) {
			@Override
			protected void logRequest(URI uri) {
				GolrSeedingDataProvider.this.logRequest(uri);
			}
		};
	}

	protected void logRequest(URI uri) {
		// hook for logging requests
	}
	
	@Override
	public Map<Bioentity, List<GeneAnnotation>> getGeneProducts(String bp) throws IOException {
		List<String[]> tagvalues = new ArrayList<String[]>();
		tagvalues.add(new String[]{"annotation_class", bp});
		addRestrictions(tagvalues); // taxon, evidence, not
		List<GolrAnnotationDocument> golrAnnotations = golr.getGolrAnnotations(tagvalues);
		GafDocument gafDocument = golr.convert(golrAnnotations);
		Map<Bioentity, List<GeneAnnotation>> result = new HashMap<Bioentity, List<GeneAnnotation>>();
		for(GeneAnnotation annotation : gafDocument.getGeneAnnotations()) {
			Bioentity bioentity = annotation.getBioentityObject();
			List<GeneAnnotation> relatedAnnotations = result.get(bioentity);
			if (relatedAnnotations == null) {
				relatedAnnotations = new ArrayList<GeneAnnotation>();
				result.put(bioentity, relatedAnnotations);
			}
			relatedAnnotations.add(annotation);
		}
		return result;
	}

	@Override
	public Map<Bioentity, List<GeneAnnotation>> getFunctions(Set<Bioentity> entities) throws IOException {
		Map<Bioentity, List<GeneAnnotation>> results = new HashMap<Bioentity, List<GeneAnnotation>>();
		Map<String, Bioentity> map = new HashMap<String, Bioentity>();
		String[] bioentityFq = new String[entities.size() + 1];
		int bioentityFqCounter = 1;
		bioentityFq[0] = "bioentity";
		for (Bioentity bioentity : entities) {
			map.put(bioentity.getId(), bioentity);
			bioentityFq[bioentityFqCounter] = bioentity.getId();
			bioentityFqCounter += 1;
		}
		List<String[]> tagvalues = new ArrayList<String[]>();
		tagvalues.add(bioentityFq);
		tagvalues.add(new String[]{"aspect", "F"}); // functions
		addRestrictions(tagvalues); // taxon, evidence, not
		
		List<GolrAnnotationDocument> golrAnnotations = golr.getGolrAnnotations(tagvalues);
		if (golrAnnotations != null && !golrAnnotations.isEmpty()) {
			GafDocument doc = new GafDocument(null, null);
			golr.convert(golrAnnotations, map, doc);
			List<GeneAnnotation> annotations = doc.getGeneAnnotations();
			if (annotations != null && !annotations.isEmpty()) {
				for (GeneAnnotation annotation : annotations) {
					if(blackList.contains(annotation.getCls())) {
						continue;
					}
					String bioentityId = annotation.getBioentity();
					Bioentity bioentity = map.get(bioentityId);
					if (bioentity != null) {
						List<GeneAnnotation> relatedAnnotations = results.get(bioentity);
						if (relatedAnnotations == null) {
							relatedAnnotations = new ArrayList<GeneAnnotation>();
							results.put(bioentity, relatedAnnotations);
						}
						relatedAnnotations.add(annotation);
					}
				}
			}
		}
		return results;
	}

	@Override
	public Map<Bioentity, List<GeneAnnotation>> getLocations(Set<Bioentity> entities) throws IOException {
		Map<Bioentity, List<GeneAnnotation>> results = new HashMap<Bioentity, List<GeneAnnotation>>();
		Map<String, Bioentity> map = new HashMap<String, Bioentity>();
		String[] bioentityFq = new String[entities.size() + 1];
		int bioentityFqCounter = 1;
		bioentityFq[0] = "bioentity";
		for (Bioentity bioentity : entities) {
			map.put(bioentity.getId(), bioentity);
			bioentityFq[bioentityFqCounter] = bioentity.getId();
			bioentityFqCounter += 1;
		}
		List<String[]> tagvalues = new ArrayList<String[]>();
		tagvalues.add(bioentityFq);
		tagvalues.add(new String[]{"-aspect", "F"}); // not function
		tagvalues.add(new String[]{"-aspect", "P"}); // not process
		addRestrictions(tagvalues); // taxon, evidence, not
		List<GolrAnnotationDocument> golrAnnotations = golr.getGolrAnnotations(tagvalues);
		if (golrAnnotations != null && !golrAnnotations.isEmpty()) {
			GafDocument doc = new GafDocument(null, null);
			golr.convert(golrAnnotations, map, doc);
			List<GeneAnnotation> annotations = doc.getGeneAnnotations();
			if (annotations != null && !annotations.isEmpty()) {
				for (GeneAnnotation annotation : annotations) {
					String bioentityId = annotation.getBioentity();
					Bioentity bioentity = map.get(bioentityId);
					if (bioentity != null && isLocation(annotation)) {
						List<GeneAnnotation> relatedAnnotations = results.get(bioentity);
						if (relatedAnnotations == null) {
							relatedAnnotations = new ArrayList<GeneAnnotation>();
							results.put(bioentity, relatedAnnotations);
						}
						relatedAnnotations.add(annotation);
					}
				}
			}
		}
		return results;
	}

	/**
	 * Check that the gene annotation is a location. Does not rely on the aspect!
	 * 
	 * @param annotation
	 * @return list of annotations
	 */
	private boolean isLocation(GeneAnnotation annotation) {
		final String clsId = annotation.getCls();
		final OWLClass cls = graph.getOWLClassByIdentifier(clsId);
		if (cls != null) {
			Set<OWLClass> superClasses = reasoner.getSuperClasses(cls, false).getFlattened();
			for (OWLClass locationRoot : locationRoots) {
				if (superClasses.contains(locationRoot)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void addRestrictions(List<String[]> tagvalues) {
		if (evidenceRestriction != null) {
			tagvalues.add(createTagValues("evidence_type_closure", evidenceRestriction));
		}
		if (taxonRestriction != null) {
			tagvalues.add(createTagValues("taxon_closure", taxonRestriction));
		}
		tagvalues.add(new String[]{"-qualifier","not"}); // exclude not annotations
	}
	
	private String[] createTagValues(String tag, Collection<String> values) {
		String[] result = new String[values.size()+1];
		result[0] = tag;
		int i = 1;
		for (String value : values) {
			result[i] = value;
			i += 1;
		}
		return result;
	}
}

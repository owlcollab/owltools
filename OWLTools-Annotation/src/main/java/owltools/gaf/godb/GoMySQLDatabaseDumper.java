package owltools.gaf.godb;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.parser.BuilderTools;
//import owltools.gaf.WithInfo;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.graph.OWLQuantifiedProperty;

/**
 * implements DatabaseDumper for GO MySQL db
 * 
 * @author cjm
 *
 */
public class GoMySQLDatabaseDumper extends DatabaseDumper {

	private static Logger LOG = Logger.getLogger(DatabaseDumper.class);

	private OWLObjectProperty is_a = null;


	protected enum GOMySQLTable {
		// go general
		db, dbxref,

		// go meta
		term2term_metadata,
		term_dbxref,
		term_definition,
		term_subset,
		term_synonym,

		// go graph
		term, term2term,

		// go association
		association,
		assassociation_property,
		association_qualifier,
		association_species_qualifier,
		evidence,
		evidence_dbxref,
		gene_product,
		gene_product_subset,
		gene_product_synonym,
		species,

		// opt

		gene_product_count,
		graph_path

	};


	/**
	 * @param g
	 */
	public GoMySQLDatabaseDumper(OWLGraphWrapper g) {
		graph = g;
	}



	/**
	 * dumps all tables
	 * @throws IOException 
	 * @throws ReferentialIntegrityException 
	 * 
	 */
	public void dump() throws IOException, ReferentialIntegrityException {
		dumpGraphModule();
		dumpMetaModule();
		dumpAssociationModule();

		dumpOptModule();

		// always do this one last
		dumpDbxrefTable();
		showStats();

		cleanup();

	}

	// ----------
	// GO GRAPH MODULE
	// ----------

	private Set<OWLClass> getAllGOTerms() {
		Set<OWLClass> objs = new HashSet<OWLClass>();
		for ( OWLClass c : graph.getAllOWLClasses() ) {
			String id = graph.getIdentifier(c);
			if (id.startsWith("NCBITaxon:")) {
				continue;
			}
			objs.add(c);
		}
		return objs;
	}

	/**
	 * @throws IOException
	 * @throws ReferentialIntegrityException
	 */
	public void dumpGraphModule() throws IOException, ReferentialIntegrityException {
		dumpTermTable();
		dumpTerm2TermTable();

	}


	/**
	 * @throws IOException
	 * @throws ReferentialIntegrityException
	 */
	public void dumpTermTable() throws IOException, ReferentialIntegrityException {
		PrintStream s = getPrintStream(GOMySQLTable.term.toString());
		for ( OWLClass c : getAllGOTerms() ) {
			dumpTermRow(s, c);
		}
		for ( OWLObjectProperty p : graph.getSourceOntology().getObjectPropertiesInSignature() ) {
			dumpTermRow(s, p);
		}
		dumpTermRow(s, getSubClassAsObjectProperty());
	}

	/**
	 * @param termStream
	 * @param obj
	 * @throws ReferentialIntegrityException
	 * @throws IOException 
	 */
	private void dumpTermRow(PrintStream termStream, OWLObject obj) throws ReferentialIntegrityException, IOException {
		Integer id = getId(GOMySQLTable.term, obj);
		String label = graph.getLabel(obj);
		if (obj.equals(getSubClassAsObjectProperty()))
			label = "is_a";

		String ns = "TODO";
		dumpRow(termStream, 
				id, 
				label,
				ns,
				graph.getIdentifier(obj),
				(graph.isObsolete(obj) ? 1 : 0),
				(graph.getOutgoingEdges(obj).size() == 0 ? 1 : 0),
				(obj instanceof OWLObjectProperty ? 1 : 0)
				);

		dumpTermSynonymRows(getPrintStream(GOMySQLTable.term_synonym.toString()),
				id, obj);
	}



	/**
	 * @throws IOException
	 * @throws ReferentialIntegrityException
	 */
	public void dumpTermSubsetTable() throws IOException, ReferentialIntegrityException {
		PrintStream s = getPrintStream(GOMySQLTable.term_subset.toString());
		for ( OWLClass c : getAllGOTerms() ) {
			dumpTermSubsetRows(s, c);
		}
		for ( OWLObjectProperty p : graph.getSourceOntology().getObjectPropertiesInSignature() ) {
			dumpTermSubsetRows(s, p);
		}
	}
	// TODO
	/**
	 * @param termStream
	 * @param obj
	 * @throws ReferentialIntegrityException
	 * @throws IOException 
	 */
	private void dumpTermSubsetRows(PrintStream s, OWLObject obj) throws ReferentialIntegrityException, IOException {
		Integer term_id = getId(GOMySQLTable.term, obj);
		List<String> subsets = graph.getSubsets(obj);
		for (String subset : subsets) {
			Integer subset_id = getTermInternalId(subset);
			dumpRow(s, 
					term_id,
					subset_id);
		}
	}


	private void dumpTermSynonymRows(PrintStream s, Integer term_id, OWLObject obj) throws ReferentialIntegrityException, IOException {
		List<ISynonym> syns = graph.getOBOSynonyms(obj);
		// TODO - dump synonymtypedefs, modeled as APs
		if (syns != null) {
			for (ISynonym syn : syns) {
				String scope = syn.getScope();
				String cat = syn.getCategory();
				dumpRow(s, 
						term_id,
						syn.getLabel(),
						null,
						this.getTermInternalId(scope, scope, "synonym_scope"),
						this.getTermInternalId(cat, cat, "synonym_type")
						);

			}
		}

	}



	/**
	 * @throws IOException
	 * @throws ReferentialIntegrityException
	 */
	public void dumpTerm2TermTable() throws IOException, ReferentialIntegrityException {
		PrintStream s = getPrintStream(GOMySQLTable.term2term.toString());
		for ( OWLClass c : this.getAllGOTerms()) {
			dumpTerm2TermRows(s, c);
		}
		for ( OWLObjectProperty p : graph.getSourceOntology().getObjectPropertiesInSignature() ) {
			dumpTerm2TermRows(s, p);
		}
		closePrintStream(GOMySQLTable.term2term.toString());
	}

	/**
	 * @param s
	 * @param obj
	 * @throws ReferentialIntegrityException
	 */
	private void dumpTerm2TermRows(PrintStream s, OWLObject obj) throws ReferentialIntegrityException {
		for (OWLGraphEdge edge : graph.getOutgoingEdges(obj)) {
			OWLQuantifiedProperty qp = edge.getSingleQuantifiedProperty();
			OWLProperty p = qp.getProperty();
			if (qp.isSubClassOf()) {
				p = getSubClassAsObjectProperty();
			}
			Integer term1_id = getId(GOMySQLTable.term, edge.getTarget(), true);
			Integer term2_id = getId(GOMySQLTable.term, edge.getSource(), true);
			Integer relationship_type_id = getId(GOMySQLTable.term, p);
			dumpRow(s, relationship_type_id,  term1_id,  term2_id, 0);
		}
	}

	// ----------
	// GO META MODULE
	// ----------

	/**
	 * @throws IOException
	 * @throws ReferentialIntegrityException
	 */
	public void dumpMetaModule() throws IOException, ReferentialIntegrityException {
		dumpTermSubsetTable();
	}


	/**
	 * @throws IOException
	 * @throws ReferentialIntegrityException
	 */
	public void dumpDbxrefTable() throws IOException, ReferentialIntegrityException {
		PrintStream s = getPrintStream(GOMySQLTable.dbxref.toString());

		//  dbxrefs are always dependent on some other table, all references
		// should have been populated, if this is called last
		Map<Object, Integer> m = tableObjIdMap.get(GOMySQLTable.dbxref.toString());
		Set<Object> xs = m.keySet();
		Iterator<Object> it = xs.iterator();
		while (it.hasNext()) {
			Object x = it.next();
			LOG.info("DBXREF: "+x);
			dumpDbxrefRow(s, x.toString());
		}
	}


	private void dumpDbxrefRow(PrintStream s, String x) throws ReferentialIntegrityException {
		Integer id = getId(GOMySQLTable.dbxref, x, true);
		int p = x.indexOf(":");
		String db = StringUtils.substring(x, 0, p);
		String acc = StringUtils.substring(x, p+1, -1);
		dumpRow(s, 
				id, 
				db,
				acc,
				null,
				null
				);
	}



	// ----------
	// GO OPT MODULE
	// ----------

	public void dumpOptModule() throws IOException, ReferentialIntegrityException {
		dumpGraphPathTable();
	}


	/**
	 * @throws IOException
	 * @throws ReferentialIntegrityException
	 */
	public void dumpGraphPathTable() throws IOException, ReferentialIntegrityException {
		PrintStream s = getPrintStream(GOMySQLTable.graph_path.toString());
		for ( OWLClass c : this.getAllGOTerms()) {
			dumpGraphPathRows(s, c);
		}
		for ( OWLObjectProperty p : graph.getSourceOntology().getObjectPropertiesInSignature() ) {
			dumpGraphPathRows(s, p);
		}
		this.closePrintStream(GOMySQLTable.graph_path.toString());
	}

	private void dumpGraphPathRows(PrintStream s, OWLObject obj) throws ReferentialIntegrityException {
		for (OWLGraphEdge edge : graph.getOutgoingEdgesClosureReflexive(obj)) {
			if (edge.getQuantifiedPropertyList().size() > 1) {
				continue;
			}
			if (! (edge.getTarget() instanceof OWLNamedObject)) {
				continue;
			}
			OWLQuantifiedProperty qp = edge.getSingleQuantifiedProperty();
			OWLProperty p = qp.getProperty();
			if (qp.isSubClassOf()) {
				p = getSubClassAsObjectProperty();
			}

			Integer term1_id = getId(GOMySQLTable.term, edge.getTarget(), true);
			Integer term2_id = getId(GOMySQLTable.term, edge.getSource(), true);
			Integer relationship_type_id = getId(GOMySQLTable.term, p);
			dumpRow(s, relationship_type_id,  term1_id,  term2_id, 
					edge.getDistance(), edge.getDistance());
		}
	}

	/**
	 * @return
	 */
	private OWLProperty getSubClassAsObjectProperty() {
		if (is_a != null)
			return is_a;
		OWLDataFactory df = graph.getDataFactory();
		is_a = df.getOWLObjectProperty(IRI.create("http://foo.org#is_a"));
		OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(
				df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), 
				is_a.getIRI(),
				df.getOWLLiteral("is_a"));
		graph.getManager().addAxiom(graph.getSourceOntology(), ax);
		return is_a;
	}



	// ----------
	// GO ASSOC MODULE
	// ----------



	public void dumpAssociationModule() throws IOException, ReferentialIntegrityException {
		dumpSpeciesTable();
		dumpAssociationTable();
	}




	public void dumpSpeciesTable() throws IOException, ReferentialIntegrityException {

		Set<String> taxIds = new HashSet<String>();
		for ( GafDocument gd : gafdocs ) {
			for (Bioentity e : gd.getBioentities()) {
				String taxId = e.getNcbiTaxonId();
				taxIds.add(taxId);
			}
		}
		LOG.info("# used taxIds = " + taxIds.size());

		for (OWLClass c : graph.getAllOWLClasses()) {
			String id = graph.getIdentifier(c);
			if (id.startsWith("NCBITaxon:")) {
				taxIds.add(id);
			}
		}
		LOG.info("#  taxIds (total) = " + taxIds.size());

		String sn = GOMySQLTable.species.toString();
		PrintStream s = getPrintStream(sn);
		for ( String taxId : taxIds ) {
			dumpSpeciesRow(s, taxId);
		}
		this.closePrintStream(sn);
	}

	private OWLObject getSpeciesObject(String taxId) {
		//String oboClsId = "NCBITaxon:"+taxId;
		OWLObject obj = graph.getOWLClassByIdentifier(taxId);
		LOG.info("Species obj = "+obj);
		return obj;
	}

	public void dumpSpeciesRow(PrintStream s, String taxId) throws IOException, ReferentialIntegrityException {
		OWLObject obj = getSpeciesObject(taxId);
		int id = getId(GOMySQLTable.species, obj);
		String binomal = graph.getLabel(obj);		
		String common_name = graph.getLabel(obj);
		String lineage_string = null;
		String genus = null;
		String species = null;
		String parent_id = null;
		String left_value = null;
		String right_value = null;
		String taxonomic_rank = null;
		if (obj != null) {
			List<ISynonym> syns = graph.getOBOSynonyms(obj);
			if (syns != null) {
				for (ISynonym syn : syns) {
					String cat = syn.getCategory();
					//LOG.info(cat + " ==> "+ syn.getLabel());
					if (cat.equals("genbank_common_name")) {
						common_name = syn.getLabel();
					}	
				}
			}
		}

		if (binomal == null) {
			addProblem("No binomal for "+taxId);
		}
		else {
			if (binomal.contains(" ")) {
				String[] parts = binomal.split(" ", 2);
				genus = parts[0];
				species = parts[1];
			}
		}

		dumpRow(s,
				id,
				taxId.replace("NCBITaxon:", ""),
				common_name,
				lineage_string,
				genus,
				species,
				parent_id,
				left_value,
				right_value,
				taxonomic_rank);

	}

	public void dumpAssociationTable() throws IOException, ReferentialIntegrityException {

		PrintStream sg = getPrintStream(GOMySQLTable.gene_product.toString());
		PrintStream s = getPrintStream(GOMySQLTable.association.toString());
		for ( GafDocument gd : gafdocs ) {
			dumpGeneProductRowsForGaf(sg, gd);
		}
		for ( GafDocument gd : gafdocs ) {
			dumpAssociationRowsForGaf(s, gd);
		}

	}

	public void dumpAssociationRowsForGaf(PrintStream s, GafDocument gafdoc) throws IOException, ReferentialIntegrityException {
		for ( GeneAnnotation a : gafdoc.getGeneAnnotations() ) {
			int id = getId(GOMySQLTable.association, a);
			OWLClass cls = graph.getOWLClassByIdentifier(a.getCls());
			if (cls == null) {
				if (isStrict) {
					throw new ReferentialIntegrityException("association", a.getCls());
				}
				this.numInvalidAnnotions++;
				continue;
			}
			Integer term1_id = getId(GOMySQLTable.term, cls, true);
			Integer gp_id = getId(GOMySQLTable.gene_product, a.getBioentity()); // TODO - force
			Integer source_db_id = getId(GOMySQLTable.db, a.getSource());
			// TODO
			dumpRow(s,
					id,
					term1_id,
					gp_id,
					a.isNegated(),
					a.getLastUpdateDate(),
					source_db_id
					);

			if (a.hasQualifiers()) {
				PrintStream s2 = getPrintStream(GOMySQLTable.association_qualifier.toString());
				if (a.hasQualifiers()) {
					if (a.isNegated()) {
						dumpRow(s2,
								id,
								this.getTermInternalId("NOT"),
								null
								);
					}
					if (a.isContributesTo()) {
						dumpRow(s2,
								id,
								this.getTermInternalId("contributes_to"),
								null
								);
					}
					if (a.isIntegralTo()) {
						dumpRow(s2,
								id,
								this.getTermInternalId("integral_to"),
								null
								);
					}
				}
			}

			PrintStream se = getPrintStream(GOMySQLTable.evidence.toString());
			Integer eid = id; // TODO
			List<String> refs = a.getReferenceIds();
			dumpRow(se,
					eid,
					a.getShortEvidence(),
					id,
					getId(GOMySQLTable.dbxref, refs.get(0)),
					BuilderTools.buildWithString(a.getWithInfos(), "")
					);

			PrintStream sedx = getPrintStream(GOMySQLTable.evidence_dbxref.toString());
			for (String w : a.getWithInfos()) {
				dumpRow(sedx,
						eid,
						getId(GOMySQLTable.dbxref, w)
						);
			}
		}
	}

	public void dumpGeneProductRowsForGaf(PrintStream s, GafDocument gafdoc) throws IOException, ReferentialIntegrityException {
		for ( Bioentity e : gafdoc.getBioentities() ) {
			int id = getId(GOMySQLTable.gene_product, e.getId());
			Integer dbxref_id = getId(GOMySQLTable.dbxref, e.getId()); // TODO - collect
			Integer species_id = getId(GOMySQLTable.species, getSpeciesObject(e.getNcbiTaxonId()), true);
			Integer type_id = getId(GOMySQLTable.term, e.getTypeCls());
			// TODO
			dumpRow(s,
					id,
					e.getSymbol(),
					species_id,
					type_id,
					e.getFullName()
					);
		}
	}

	// -------
	// UTIL
	// -------

	Map<String, OWLObject> owlObjectById = new HashMap<String, OWLObject>();
	private OWLObject getTerm(String id, String name, String ns) throws IOException, ReferentialIntegrityException {
		if (!owlObjectById.containsKey(id)) {
			IRI iri = graph.getIRIByIdentifier(id);
			OWLClass obj = graph.getDataFactory().getOWLClass(iri);
			owlObjectById.put(id, obj);
			PrintStream s = getPrintStream(GOMySQLTable.term.toString(), true);
			dumpTermRow(s, obj);
			// TODO - name and ns
		}
		return owlObjectById.get(id);
	}


	private Integer getTermInternalId(String id) throws ReferentialIntegrityException, IOException {
		return getTermInternalId(id, id, null);
	}
	private Integer getTermInternalId(String id, String name, String ns) throws ReferentialIntegrityException, IOException {
		if (id == null)
			return null;
		return getId(GOMySQLTable.term, getTerm(id, name, ns));
	}

	private Integer getId(GOMySQLTable t, Object obj) throws ReferentialIntegrityException {
		return getId(t.toString(), obj);
	}

	private Integer getId(GOMySQLTable t, Object obj, boolean check) throws ReferentialIntegrityException {
		return getId(t.toString(), obj, check);
	}



}

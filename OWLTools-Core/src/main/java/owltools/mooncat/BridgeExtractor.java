package owltools.mooncat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * @author cjm
 *
 * Extracts bridge ontologies from an ontology. A bridge ontology consists solely of class axioms where
 * the signature of the axiom contains classes that belong to two or more distinct ontologies, together with
 * any necessary object property axioms
 * <p>
 * Here the notion of belonging is determined by IRI - e.g. GO_nnnnn belongs to go.
 * <p>
 * This procedure enforces a naming convention whereby the bridge ontology is called
 * <p>
 *   &lt;srcOntId&gt;-bridge-to-&lt;xOnt&gt;
 * <p>
 * If an axiom bridges two or more ontologies, then specialized ontologies of the form
 * <p> 
 *     &lt;srcOntId&gt;-bridge-to-&lt;xOnt1&gt;-and...-and-&lt;xOntN&gt;
 * <p>    
 * are created
 * <p>
 * In addition, an importer ontology is created
 */
public class BridgeExtractor {

	OWLOntology ontology;
	public String subDir = "bridge/";
	private Logger LOG = Logger.getLogger(BridgeExtractor.class);
	Map<String,OWLOntology> nameToOntologyMap;
	OWLOntology importOntology;
	Set<Combo> combos;
	
	/**
	 * Maps a set of ontologies (e.g. cl, uberon) to a single target (e.g. uberon-plus-cl)
	 *
	 */
	public class Combo {
		Set<String> srcOntIds;
		String tgtOntId;
		
		public Combo(Set<String> srcOntIds, String tgtOntId) {
			super();
			this.srcOntIds = srcOntIds;
			this.tgtOntId = tgtOntId;
		}
		/**
		 * if srcOntIds is a subset of xOntIds, then replace that subset
		 * with tgtOntId
		 * 
		 * @param xOntIds
		 */
		public void reduce(List<String> xOntIds) {
			List<String> x = new ArrayList<String>(xOntIds);
			x.removeAll(srcOntIds);
			if ((xOntIds.size() - x.size()) == srcOntIds.size()) {
				xOntIds.removeAll(srcOntIds);
				xOntIds.add(tgtOntId);
				Collections.sort(xOntIds);
			}
		}
	}

	public BridgeExtractor(OWLOntology ontology) {
		super();
		this.ontology = ontology;
	}
	
	public void addCombo(String tgtOntId, Set<String> srcOntIds) {
		Combo combo = new Combo(srcOntIds, tgtOntId);
		if (combos == null)
			combos = new HashSet<Combo>();
		combos.add(combo);
	}

	/**
	 * given a source ontology O:
	 * <p>
	 * For each axiom A in O :
	 * <ul>
	 *  <li>get signature of A</li>
	 *  <li>for every object in signature, calculate the set of ontologies these objects belong to</li>
	 *  <li>if >1 ontologies, then add the axiom to a bridge ontology dedicated to this list of ontologies
	 *    <ul>
	 *      <li>add any required object properties</li>
	 *      <li>optionally remove the axiom from the source</li>
	 *    </ul>
	 *  </li>
	 * </ul>
	 * @param srcOntId
	 * @param isRemoveBridgeAxiomsFromSource
	 * @return ontology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology extractBridgeOntologies(String srcOntId, boolean isRemoveBridgeAxiomsFromSource) throws OWLOntologyCreationException {
		nameToOntologyMap = new HashMap<String,OWLOntology>();

		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLLogicalAxiom ax : ontology.getLogicalAxioms()) {

			List<String> xOntIds = new ArrayList<String>();
			Set<OWLClass> cs = ax.getClassesInSignature();
			for (OWLClass c : cs) {
				String xOntId = getOntId(c);
				if (!xOntId.equals(srcOntId) && !xOntIds.contains(xOntId))
					xOntIds.add(xOntId);
			}
			for (OWLNamedIndividual i : ax.getIndividualsInSignature()) {
				String xOntId = getOntId(i);
				if (!xOntId.equals(srcOntId) && !xOntIds.contains(xOntId))
					xOntIds.add(xOntId);
			}
			if (xOntIds.size() > 0) {
				// bridge ontology connects 2 or more
				Collections.sort(xOntIds);
				OWLOntology xOnt = getBridgeOntology(srcOntId, xOntIds);
				if (isRemoveBridgeAxiomsFromSource) {
					rmAxioms.add(ax);
				}
				ontology.getOWLOntologyManager().addAxiom(xOnt,ax);
				for (OWLObjectProperty p : ax.getObjectPropertiesInSignature()) {
					addObjectProperty(p, xOnt);
				}
			}
		}
		ontology.getOWLOntologyManager().removeAxioms(ontology, rmAxioms);
		
		// make importer
		IRI xIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+srcOntId+"/importer.owl");
		importOntology = ontology.getOWLOntologyManager().createOntology(xIRI);

		OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
		for (OWLOntology xo : nameToOntologyMap.values()) {
			AddImport ai = 
				new AddImport(importOntology,
						df.getOWLImportsDeclaration(xo.getOntologyID().getOntologyIRI()));
			importOntology.getOWLOntologyManager().applyChange(ai);
		}
		LOG.info("Getting importer: "+importOntology);
		AddImport ai = 
			new AddImport(importOntology,
					df.getOWLImportsDeclaration(IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+srcOntId+"/core.owl")));
		importOntology.getOWLOntologyManager().applyChange(ai);
		
		return importOntology;
	}

	private void addObjectProperty(OWLObjectProperty p, OWLOntology xOnt) {
		if (xOnt.getDeclarationAxioms(p).size() > 0) {
			return;
		}
		OWLOntologyManager m = ontology.getOWLOntologyManager();
		OWLDataFactory df = m.getOWLDataFactory();
		m.addAxiom(xOnt, df.getOWLDeclarationAxiom(p));
		for (OWLAxiom ax : ontology.getAxioms(p)) {
			m.addAxiom(xOnt, ax);
		}
		// TODO

	}

	private OWLOntology getBridgeOntology(String srcOntId, List<String> xOntIds) throws OWLOntologyCreationException {
		StringBuffer n = new StringBuffer(srcOntId + "-bridge-to-");
		if (combos != null) {
			for (Combo combo: combos) {
				combo.reduce(xOntIds);
			}
		}
		int i = 0;
		for (String xo : xOntIds) {
			if (i>0)
				n.append("-and-");
			n.append(xo);
			i++;
		}
		IRI xIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+srcOntId+"/bridge/"+n+".owl");
		OWLOntology xo = ontology.getOWLOntologyManager().getOntology(xIRI);
		if (xo == null) {
			LOG.info("Creating "+xIRI);
			xo = ontology.getOWLOntologyManager().createOntology(xIRI);
			nameToOntologyMap.put(n.toString(), xo);
		}
		return xo;
	}

	private String getOntId(OWLNamedObject c) {
		String iriStr = c.getIRI().toString();
		iriStr = iriStr.replaceAll(".*/", ""); // up to final slash
		iriStr = iriStr.replaceAll("_\\d+",""); // assumes obo-style
		//LOG.info(c + " ==> "+iriStr);
		return iriStr.toLowerCase();
	}

	public void saveBridgeOntologies(String dir) throws FileNotFoundException, OWLOntologyStorageException {
		saveBridgeOntologies(dir, new RDFXMLOntologyFormat());
	}

	public void saveBridgeOntologies(String dir, OWLOntologyFormat format) throws FileNotFoundException, OWLOntologyStorageException {
		for (String n : nameToOntologyMap.keySet()) {
			OWLOntology xo = nameToOntologyMap.get(n);
			String fn = dir == null ?  "bridge/" + n : dir + "/bridge/" + n;
			save(fn, format, xo);
		}
		String n = "importer";
		String ifn = dir == null ? n : dir + "/"+n;
		save(ifn, format, importOntology);
	}
	
	public void save(String fn, OWLOntologyFormat format, OWLOntology xo) throws FileNotFoundException, OWLOntologyStorageException {
		fn = fn + "." + getSuffix(format);
		File file = new File(fn);
		file.getParentFile().mkdirs();
		OutputStream os = new FileOutputStream(file);
		LOG.info("Saving: "+xo);
		ontology.getOWLOntologyManager().saveOntology(xo, format, os);
	}
	
	

	private String getSuffix(OWLOntologyFormat format) {
		if (format instanceof RDFXMLOntologyFormat) {
			return "owl";
		}
		if (format instanceof OWLFunctionalSyntaxOntologyFormat) {
			return "ofn";
		}
		if (format instanceof OWLXMLOntologyFormat) {
			return "owx";
		}
		if (format instanceof ManchesterOWLSyntaxOntologyFormat) {
			return "omn";
		}

		return "owl";
	}

}

package owltools.gaf.godb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

/**
 * implements DatabaseDumper for GO MySQL db
 * 
 * @author cjm
 *
 */
public class GoMySQLDatabaseDumper extends DatabaseDumper {

	
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
		species
		};


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
		dumpAssociationModule();

	}

	// ----------
	// GO GRAPH MODULE
	// ----------

	public void dumpGraphModule() throws IOException, ReferentialIntegrityException {
		dumpTermTable();
		dumpTerm2TermTable();

	}
	

	public void dumpTermTable() throws IOException, ReferentialIntegrityException {
		PrintStream s = getPrintStream(GOMySQLTable.term.toString());
		for ( OWLClass c : graph.getAllOWLClasses() ) {
			dumpTermRow(s, c);
		}
		for ( OWLObjectProperty p : graph.getSourceOntology().getObjectPropertiesInSignature() ) {
			dumpTermRow(s, p);
		}
		s.close();

	}


	private void dumpTermRow(PrintStream termStream, OWLObject obj) throws ReferentialIntegrityException {
		Integer id = getId(GOMySQLTable.term, obj);
		String label = graph.getLabel(obj);
		String ns = "TODO";
		dumpRow(termStream, 
				id, 
				graph.getLabel(obj),
				ns,
				graph.getIdentifier(obj),
				(graph.isObsolete(obj) ? 1 : 0),
				(graph.getOutgoingEdges(obj).size() == 0 ? 1 : 0),
				(obj instanceof OWLObjectProperty ? 1 : 0)
				);
	}


	public void dumpTerm2TermTable() throws IOException, ReferentialIntegrityException {
		PrintStream s = getPrintStream(GOMySQLTable.term2term.toString());
		for ( OWLClass c : graph.getAllOWLClasses() ) {
			dumpRelationshipRows(s, c);
		}
		for ( OWLObjectProperty p : graph.getSourceOntology().getObjectPropertiesInSignature() ) {
			dumpRelationshipRows(s, p);
		}
		s.close();
	}

	private void dumpRelationshipRows(PrintStream s, OWLObject obj) throws ReferentialIntegrityException {
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

	private OWLProperty getSubClassAsObjectProperty() {
		// TODO Auto-generated method stub
		return null;
	}

	private void dumpTerm2TermRow(PrintStream termStream, OWLObject obj) throws ReferentialIntegrityException {
		Integer id = getId(GOMySQLTable.term2term, obj);
		String label = graph.getLabel(obj);
		dumpRow(termStream, id, label);
	}

	// ----------
	// GO ASSOC MODULE
	// ----------

	
	
	public void dumpAssociationModule() throws IOException, ReferentialIntegrityException {
		dumpAssociationTable();

	}

	public void dumpAssociationTable() throws IOException, ReferentialIntegrityException {
		PrintStream s = getPrintStream(GOMySQLTable.association.toString());
		for ( GafDocument gd : gafdocs ) {
			dumpAssociationRowsForGaf(s, gd);
		}
		s.close();		
	}
	
	public void dumpAssociationRowsForGaf(PrintStream s, GafDocument gafdoc) throws IOException, ReferentialIntegrityException {
		for ( GeneAnnotation a : gafdoc.getGeneAnnotations() ) {
			int id = getId(GOMySQLTable.association, a);
			OWLClass cls = graph.getOWLClassByIdentifier(a.getCls());
			if (cls == null) {
				throw new ReferentialIntegrityException("association", a.getCls());
			}
			Integer term1_id = getId(GOMySQLTable.term, cls, true);
			// TODO
			dumpRow(s,
					id,
					term1_id);
					
		}
	}



	//
	

	private Integer getId(GOMySQLTable t, Object obj) throws ReferentialIntegrityException {
		return getId(t.toString(), obj);
	}
	
	private Integer getId(GOMySQLTable t, Object obj, boolean check) throws ReferentialIntegrityException {
		return getId(t.toString(), obj, check);
	}
	


}

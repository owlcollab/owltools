package owltools;

import java.io.FileOutputStream;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.io.ParserWrapper;
import owltools.util.OwlHelper;

public class FlyTutorial extends OWLToolsTestBasics {

	@Test
	@Ignore("This test requires an external resource. This can lead to false positive failures.")
	public void demo() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("fbbt.owl"));
		OWLOntology ont = g.getSourceOntology();


		SimpleShortFormProvider sfp = new SimpleShortFormProvider();


		//OWLClass c = g.getOWLClassByIdentifier("FBbt:00003719");
		//OWLClass c = g.getOWLClass("http://purl.obolibrary.org/obo/FBbt_00003719");
		OWLClass c = g.getOWLClass("http://purl.obolibrary.org/obo/FBbt_00003743");
		String label = g.getLabel(c);
		String def = g.getDef(c);
		List<ISynonym> syns = g.getOBOSynonyms(c);
		System.out.println("LABEL="+label);
		System.out.println("DEF="+def);
		for (String x : g.getDefXref(c)) {
			System.out.println("  DEF_XREF="+x);
		}
		for (ISynonym syn : syns) {
			System.out.println("  SYN="+syn);
			System.out.println("    SYN_LABEL="+syn.getLabel());
			System.out.println("    SCOPE="+syn.getScope());
			if (syn.getXrefs() != null) {
				for (String x : syn.getXrefs()) {
					System.out.println("      SYN_XREF="+x);				
				}
			}
		}
		/*
		//OWLClass c = (OWLClass) g.getOWLObjectByLabel("lamina monopolar neuron L1");
		for (OWLAnnotationAssertionAxiom ax : c.getAnnotationAssertionAxioms(ont)) {
			System.out.println(ax);			
		}
		 */
		for (OWLClassExpression s : OwlHelper.getSuperClasses(c, ont)) {
			if (s instanceof OWLEntity) {
				System.out.println(" SUPERCLASS:"+sfp.getShortForm((OWLEntity) s)+" // "+g.getLabel(s));
			}
			else if (s instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)s;
				OWLObjectPropertyExpression p = svf.getProperty();
				OWLClassExpression filler = svf.getFiller();
				if (p instanceof OWLProperty && filler instanceof OWLClass) {
					System.out.println(" RELATIONSHIP:");
					System.out.println("  REL:"+sfp.getShortForm((OWLProperty)p)+" // "+g.getLabel(p));
					System.out.println("  FILLER:"+sfp.getShortForm((OWLClass)filler)+" // "+g.getLabel(filler));
				}
				else {
					System.out.println("will not show complex relationships!");
				}
				//sfp.
				//System.out.println("  REL:"+svf.getProperty()+" "+g.getIdentifier(svf.getProperty())+" \""+g.getLabel(svf.getProperty())+"\" "+
				//		" FILLER:"+svf.getFiller()+" \""+g.getLabel(svf.getFiller())+"\" ");
			}
			else {

			}
		}
		/*
		for (OWLClassAxiom ax : ont.getAxioms(c)) {
			System.out.println(ax);
		}
		 */

		/*
		for (OWLClass c : ont.getClassesInSignature()) {
			for (OWLClassAxiom ax : ont.getAxioms(c)) {
				System.out.println(ax);
			}
		}
		 */
		
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);

		r.addObject(c);
		r.renderImage("png",new FileOutputStream("/tmp/neuron.png"));


	}

}

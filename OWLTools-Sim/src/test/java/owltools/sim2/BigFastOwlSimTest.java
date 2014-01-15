package owltools.sim2;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;

import com.google.common.collect.Sets;
import com.googlecode.javaewah.EWAHCompressedBitmap;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;
import owltools.sim2.preprocessor.ABoxUtils;
import owltools.sim2.scores.ElementPairScores;

/**
 * 
 * Consider also:
 * 
 * http://stackoverflow.com/questions/10784951/do-any-jvms-jit-compilers-generate-code-that-uses-vectorized-floating-point-ins
 * 
 * @author cjm
 *
 */
public class BigFastOwlSimTest extends OWLToolsTestBasics{

	OwlSim owlsim;
	
	@Test
	public void testWorm() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		t("wbphenotype", 2000);
	}
	@Test
	public void testMouse() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		t("mp", 2000);
	}
	@Test
	public void testMouseIndividuals() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		testIndividuals("mp", 100);
	}
	@Test
	public void testMouseRandomIndividuals() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		testIndividualsRandom("mp", 1000);
	}
	@Test
	public void testMammalRandomIndividuals() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		testIndividualsRandom("mammal-merged", 1000);
	}
	
	// benchmarking old vs new
	@Test
	public void testWormRandomIndividuals() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		testIndividualsRandom("wbphenotype", 1000);
	}
	@Test
	public void testWormRandomIndividualsSOS() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		testIndividualsRandom("wbphenotype", 1000, new SimpleOwlSimFactory());
	}
	@Test
	public void testMouseRandomIndividualsSOS() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		testIndividualsRandom("mp", 1000, new SimpleOwlSimFactory());
	}

	private void precompute() throws UnknownOWLClassException {
		owlsim.createElementAttributeMapFromOntology();
	}
	
	private void precomputeLCS() throws UnknownOWLClassException {
		owlsim.precomputeAttributeAllByAll();
//		Set<OWLClass> cset = owlsim.getAllAttributeClasses();
//		int n=0;
//		for (OWLClass c : cset) {
//			n++;
//			if (n % 100 ==0) {
//				msg("Pre-LCS: "+c+" Done: "+n);
//			}
//			for (OWLClass d : cset) {
//				owlsim.getLowestCommonSubsumerWithIC(c, d);
//			}
//		}

	}


	@Test
	public void testLCS() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		String idspace="mp";
		load(idspace);
		precompute();
		OWLClass c1 = getCls("MP_0003737"); // ossification of pinnae
		OWLClass c2 = getCls("MP_0002895"); // abnormal otolithic membrane morphology
		Set<Node<OWLClass>> cs = owlsim.getNamedCommonSubsumers(c1, c2);
		msg("CS="+cs);
		Set<Node<OWLClass>> lcs = owlsim.getNamedLowestCommonSubsumers(c1, c2);
		msg("LCS="+lcs);
	}
	
	public void t(String idspace, int size) throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		Runtime rt = Runtime.getRuntime();
		load(idspace);
		ABoxUtils.makeDefaultIndividuals(getOntology());
		long usedMB0 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		precompute();
		long t1 = System.currentTimeMillis();
		long usedMB1 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("LOADED: "+idspace+"  NUM:"+size+" usedMB:"+usedMB0);
		msg("TEST: "+idspace+"  NUM:"+size+" usedMB:"+usedMB1);
		axa(size);
		long t2 = System.currentTimeMillis();
		long usedMB2 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("COMPLETED TEST: "+idspace+"  NUM:"+size+" usedMB:"+usedMB2+
				" usedMBDelta:" + (usedMB2-usedMB1) +
				" TIME (ms):"+(t2-t1));		

		if (idspace.equals("mp")) {
			OWLClass c1 = getCls("MP_0003737"); // ossification of pinnae
			OWLClass c2 = getCls("MP_0002895"); // abnormal otolithic membrane morphology
			assert(1 == owlsim.getAttributeJaccardSimilarity(c1, c1));
			assert(1 == owlsim.getAttributeJaccardSimilarity(c2, c2));
			msg("SIMTEST="+owlsim.getAttributeJaccardSimilarity(c1, c2));
		}
	}
	
	public void testIndividuals(String idspace, int size) throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		Runtime rt = Runtime.getRuntime();
		load(idspace);
		ABoxUtils.makeDefaultIndividuals(getOntology(), "-proto");
		long usedMB0 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		precompute();
		precomputeLCS();
		long t1 = System.currentTimeMillis();
		long usedMB1 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("LOADED: "+idspace+"  NUM:"+size+" usedMB:"+usedMB0);
		msg("TEST: "+idspace+"  NUM:"+size+" usedMB:"+usedMB1);
		ixi(size);
		long t2 = System.currentTimeMillis();
		long usedMB2 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("COMPLETED TEST: "+idspace+"  NUM:"+size+" usedMB:"+usedMB2+
				" usedMBDelta:" + (usedMB2-usedMB1) +
				" TIME (ms):"+(t2-t1));		

		if (idspace.equals("mp")) {
			OWLClass c1 = getCls("MP_0003737"); // ossification of pinnae
			OWLClass c2 = getCls("MP_0002895"); // abnormal otolithic membrane morphology
			assert(1 == owlsim.getAttributeJaccardSimilarity(c1, c1));
			assert(1 == owlsim.getAttributeJaccardSimilarity(c2, c2));
			msg("SIMTEST="+owlsim.getAttributeJaccardSimilarity(c1, c2));
		}
	}

	public void testIndividualsRandom(String idspace, int size) throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		testIndividualsRandom(idspace, size, new FastOwlSimFactory());
	}
	
	public void testIndividualsRandom(String idspace, int size, OwlSimFactory simFactory) throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		Runtime rt = Runtime.getRuntime();
		load(idspace, simFactory);
		//ABoxUtils.makeDefaultIndividuals(getOntology(), "-proto");
		ABoxUtils.createRandomClassAssertions(owlsim.getSourceOntology(), size, 10);
		long usedMB0 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		precompute();
		precomputeLCS();
		long t1 = System.currentTimeMillis();
		long usedMB1 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("LOADED: "+idspace+"  NUM:"+size+" usedMB:"+usedMB0);
		msg("TEST: "+idspace+"  NUM:"+size+" usedMB:"+usedMB1);
		ixi(size);
		long t2 = System.currentTimeMillis();
		long usedMB2 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("COMPLETED TEST: "+idspace+" SF: "+simFactory+"  NUM:"+size+" usedMB:"+usedMB2+
				" usedMBDelta:" + (usedMB2-usedMB1) +
				" TIME (ms):"+(t2-t1));		

		if (idspace.equals("mp")) {
			OWLClass c1 = getCls("MP_0003737"); // ossification of pinnae
			OWLClass c2 = getCls("MP_0002895"); // abnormal otolithic membrane morphology
			assert(1 == owlsim.getAttributeJaccardSimilarity(c1, c1));
			assert(1 == owlsim.getAttributeJaccardSimilarity(c2, c2));
			msg("SIMTEST="+owlsim.getAttributeJaccardSimilarity(c1, c2));
		}
	}

	public void testLCS(String idspace, int size) throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
		Runtime rt = Runtime.getRuntime();
		load(idspace);
		ABoxUtils.makeDefaultIndividuals(getOntology());
		long usedMB0 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		precompute();
		long t1 = System.currentTimeMillis();
		long usedMB1 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("LCSLOADED: "+idspace+"  NUM:"+size+" usedMB:"+usedMB0);
		msg("LCSTEST: "+idspace+"  NUM:"+size+" usedMB:"+usedMB1);
		Set<OWLClass> all = getOntology().getClassesInSignature();
		Set<OWLClass> cset = new HashSet<OWLClass>();
		int i=0;
		for (OWLClass c : getOntology().getClassesInSignature()) {
			i++;
			if (i>size)
				break;
			cset.add(c);
		}
		for (OWLClass c : cset) {
			for (OWLClass d : cset) {
				owlsim.getNamedLowestCommonSubsumers(c, d);
			}
		}
		long t2 = System.currentTimeMillis();
		long usedMB2 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("LCSCOMPLETED TEST: "+idspace+"  NUM:"+size+" usedMB:"+usedMB2+
				" usedMBDelta:" + (usedMB2-usedMB1) +
				" TIME (ms):"+(t2-t1));		

	
	}

	private OWLClass getCls(String id) {
		return 
				getOntology().getOWLOntologyManager().getOWLDataFactory().
				getOWLClass(IRI.create("http://purl.obolibrary.org/obo/"+id));
	}
	
	private void ixi(int size) throws UnknownOWLClassException {
		Set<OWLNamedIndividual> all = owlsim.getAllElements();
		Set<OWLNamedIndividual> iset = new HashSet<OWLNamedIndividual>();
		int i=0;
		for (OWLNamedIndividual c : all) {
			i++;
			if (i>size)
				break;
			iset.add(c);
		}
		ixi(iset, iset);
	}
	
	public void ixi(Set<OWLNamedIndividual> iset, Set<OWLNamedIndividual> jset) throws UnknownOWLClassException {
		double total = 0;
		int n = 0;
		SummaryStatistics overallStat = new SummaryStatistics();
		for (OWLNamedIndividual i : iset) {
			SummaryStatistics stat = new SummaryStatistics();

			for (OWLNamedIndividual j : jset) {
				Set<OWLClass> cs = owlsim.getAttributesForElement(i);
				Set<OWLClass> ds = owlsim.getAttributesForElement(j);
				double s = owlsim.getElementJaccardSimilarity(i,j);			
				total += s;
				ElementPairScores gwsim = owlsim.getGroupwiseSimilarity(i, j);
				//Set<Node<OWLClass>> lcs = getNamedCommonSubsumers(c, d);
				//msg(" "+gwsim);
				//msg("  simj="+s);
				//msg("  atts="+cs+" "+ds);
				n++;
				stat.addValue(gwsim.bmaAsymIC);
			}
			msg("Mean bmaAsymIC for "+i.getIRI()+": "+overallStat.getMean());
			overallStat.addValue(stat.getMean());
		}
		msg("AVG: "+total / (double) n);
		msg("Overall Mean for bmaAsymIC:"+overallStat.getMean());
	}


	private void axa(int size) throws UnknownOWLClassException {
		Set<OWLClass> all = getOntology().getClassesInSignature();
		Set<OWLClass> cset = new HashSet<OWLClass>();
		int i=0;
		for (OWLClass c : getOntology().getClassesInSignature()) {
			i++;
			if (i>size)
				break;
			cset.add(c);
		}
		axa(cset, cset);
	}

	public void axa(Set<OWLClass> cset, Set<OWLClass> dset) throws UnknownOWLClassException {
		double total = 0;
		int n = 0;
		for (OWLClass c : cset) {
			//msg("Attr(c)="+c);
			for (OWLClass d : cset) {
				double s = owlsim.getAsymmetricAttributeJaccardSimilarity(c, d);			
				//msg(" "+c+" , "+d+" = "+s);
				total += s;
				
				//Set<Node<OWLClass>> lcs = getNamedCommonSubsumers(c, d);
				//msg(" "+c+" , "+d+" = "+lcs);
				n++;
			}
		}
		msg("AVG: "+total / (double) n);
	}

	
	protected OWLOntology getOntology() {
		return owlsim.getSourceOntology();
	}

	private void load(String idspace) throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		load(idspace, new FastOwlSimFactory());
	}
	private void load(String idspace, OwlSimFactory simfactory) throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		msg("Loading");
		ParserWrapper pw = new ParserWrapper();
		String base;
		//base = "/Users/cjm/repos/phenotype-ontologies/src/ontology/";
		base = "http://purl.obolibrary.org/obo/";
		OWLOntology ontology = pw.parseOBO(base+idspace+".obo");
		//ElkReasonerFactory reasonerFactory = new ElkReasonerFactory();
		//OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
		owlsim = simfactory.createOwlSim(ontology);
		msg("Loaded. Root = " + owlsim.getSourceOntology());
	}

	private void msg(String s) {
		System.out.println(s);
	}

	

}

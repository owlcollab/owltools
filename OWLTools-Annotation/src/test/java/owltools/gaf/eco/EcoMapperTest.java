package owltools.gaf.eco;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Basic tests for GO evidence code mappings.
 */
public class EcoMapperTest {

	@Test
	public void basicTest() throws Exception {
		ParserWrapper p = new ParserWrapper();
		OWLGraphWrapper graph = p.parseToOWLGraph(EcoMapper.ECO_PURL);
		EcoMapper mapper = EcoMapperFactory.createEcoMapper(graph);
		
		assertFalse(mapper.isGoEvidenceCode(null));
		assertFalse(mapper.isGoEvidenceCode("FOO"));
		
		assertTrue(mapper.isGoEvidenceCode("IEA"));
		assertTrue(mapper.isGoEvidenceCode("ND"));
		assertTrue(mapper.isGoEvidenceCode("IPI"));
		
		assertEquals(graph.getOWLClassByIdentifier("ECO:0000021"), mapper.getEcoClassForCode("IPI"));
		
		assertEquals(2, mapper.getAllEcoClassesForCode("IGC").size());
	}
	
	@Test
	public void checkMapping() throws Exception {
		EcoMapper mapper = EcoMapperFactory.createEcoMapper();
		
		single(mapper, "0000269", "EXP");
		single(mapper, "0000318", "IBA");
		single(mapper, "0000319", "IBD");
		single(mapper, "0000305", "IC");
		single(mapper, "0000314", "IDA");
		
		single(mapper, "0000270", "IEP");
		
		single(mapper, "0000316", "IGI");
		single(mapper, "0000320", "IKR"); // same as IMR ?
		single(mapper, "0000315", "IMP");
		single(mapper, "0000320", "IMR"); // same as IKR ?
		single(mapper, "0000021", "IPI");
		single(mapper, "0000321", "IRD");
		single(mapper, "0000247", "ISA");
		single(mapper, "0000255", "ISM");
		single(mapper, "0000266", "ISO");
		single(mapper, "0000303", "NAS");
		single(mapper, "0000307", "ND");
		single(mapper, "0000245", "RCA");
		single(mapper, "0000304", "TAS");
		
		multiple(mapper, 
				Arrays.asList("0000317", "0000084"), 
				"IGC", 
				Arrays.asList("GO_REF:0000025"));
		
		multiple(mapper, 
				Arrays.asList("0000203", "0000256", "0000203", "0000203",
						"0000265", "0000265", "0000203", "0000265",
						"0000322", "0000323", "0000322", "0000323",
						"0000265"), 
				"IEA", 
				Arrays.asList("GO_REF:0000002", "GO_REF:0000003", "GO_REF:0000004", "GO_REF:0000019",
						"GO_REF:0000020", "GO_REF:0000023", "GO_REF:0000035", "GO_REF:0000037",
						"GO_REF:0000038", "GO_REF:0000039", "GO_REF:0000040", "GO_REF:0000049"));
		
		multiple(mapper, 
				Arrays.asList("0000250", "0000255", "0000031", "0000031", "0000031"), 
				"ISS", 
				Arrays.asList("GO_REF:0000011", "GO_REF:0000012", "GO_REF:0000018", "GO_REF:0000027"));
	}
	
	private void single(EcoMapper mapper, String eco, String go) {
		final OWLClass cls = mapper.getEcoClassForCode(go);
		assertEquals("http://purl.obolibrary.org/obo/ECO_"+eco, cls.getIRI().toString());
		Set<OWLClass> codes = mapper.getAllEcoClassesForCode(go);
		assertEquals(1, codes.size());
		assertEquals(cls, codes.iterator().next());
	}
	
	private void multiple(EcoMapper mapper, List<String> ecos, String go, List<String> refs) {
		final OWLClass cls = mapper.getEcoClassForCode(go);
		final String first = ecos.get(0);
		assertEquals("http://purl.obolibrary.org/obo/ECO_"+first, cls.getIRI().toString());
		
		assertEquals(refs.size() + 1, ecos.size());
		
		Set<OWLClass> all = new HashSet<OWLClass>();
		all.add(cls);
		
		for (int i = 0; i < refs.size(); i++) {
			String ref = refs.get(i);
			String currentEco = ecos.get(i+1);
			OWLClass refCls = mapper.getEcoClassForCode(go, ref);
			assertEquals("http://purl.obolibrary.org/obo/ECO_"+currentEco, refCls.getIRI().toString());
			all.add(refCls);
		}
		
		Set<OWLClass> codes = mapper.getAllEcoClassesForCode(go);
		assertEquals(all.size(), codes.size());
		assertTrue(all.containsAll(codes));
		assertTrue(codes.containsAll(all));
	}
}

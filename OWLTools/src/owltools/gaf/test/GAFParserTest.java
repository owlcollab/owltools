
package owltools.gaf.test;

import java.io.IOException;

import org.junit.Test;

import owltools.gaf.GAFParser;
import owltools.test.OWLToolsTestBasics;

public class GAFParserTest extends OWLToolsTestBasics {

	@Test
	public void testParser() throws IOException{
		GAFParser p = new GAFParser();
		
		p.parse(getResource("test_gene_association_mgi.gaf"));
		
		while(p.next()){
			System.out.println(p.toString());
			System.out.println(p.getDb() + "\t" + p.getTaxon());
		}
	}
	
}

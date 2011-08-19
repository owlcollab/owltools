
package owltools.gaf.test;

import java.io.File;
import java.io.IOException;

import owltools.gaf.GAFParser;
import junit.framework.TestCase;

public class GAFParserTest extends TestCase {

	public static void testParser() throws IOException{
		GAFParser p = new GAFParser();
		
		p.parse(new File("test_resources/test_gene_association_mgi.gaf"));
		
		while(p.next()){
			System.out.println(p.toString());
			System.out.println(p.getDb() + "\t" + p.getTaxon());
		}
	}
	
}

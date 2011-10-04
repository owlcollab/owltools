package owltools.idmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.log4j.Logger;

/**
 * 
 * @author cjm
 * 
 */
public class IDMappingPIRParser {
	
	

	public enum Types {

		UniProtKB_accession,
		UniProtKB_ID,
		EntrezGene,
		RefSeq,
		NCBI_GI_number,
		PDB,
		Pfam,
		GO,
		PIRSF,
		IPI,
		UniRef100,
		UniRef90,
		UniRef50,
		UniParc,
		PIR_PSD_accession,
		NCBI_taxonomy,
		MIM,
		UniGene,
		Ensembl,
		PubMed_ID,
		EMBL_GenBank_DDBJ,
		EMBL_protein_id
	};
	
	public static Types[] COLS =
	{
		Types.UniProtKB_accession,
		Types.UniProtKB_ID,
		Types.EntrezGene,
		Types.RefSeq,
		Types.NCBI_GI_number,
		Types.PDB,
		Types.Pfam,
		Types.GO,
		Types.PIRSF,
		Types.IPI,
		Types.UniRef100,
		Types.UniRef90,
		Types.UniRef50,
		Types.UniParc,
		Types.PIR_PSD_accession,
		Types.NCBI_taxonomy,
		Types.MIM,
		Types.UniGene,
		Types.Ensembl,
		Types.PubMed_ID,
		Types.EMBL_GenBank_DDBJ,
		Types.EMBL_protein_id

	};
	
	public Types[] COL_SUBSET =
	{
			Types.UniProtKB_accession,
			Types.UniProtKB_ID,
			Types.EntrezGene,
			Types.RefSeq,
			Types.NCBI_GI_number,
			Types.PDB,
			Types.Pfam,
			Types.PIRSF,
			Types.MIM,
			Types.UniGene,
			Types.Ensembl,
			Types.EMBL_GenBank_DDBJ,
			Types.EMBL_protein_id			
	};

	public Set<Integer> colIxSubset = new HashSet<Integer>();
	public Map<Types,Integer> typeIxMap;
	public Map<Integer, Set<String>> filterWhere = new HashMap<Integer, Set<String>>();

	
	private BufferedReader reader;
	
	private String currentRow;
	
	private String currentCols[];
	
	private int expectedNumCols;
	
	private int lineNumber;
	
	public IDMapHandler handler;

	
	public IDMappingPIRParser(){
		init();
	}
	
	private void init(){
		typeIxMap = new HashMap<Types, Integer>();
		for (int i=0; i< COLS.length; i++) {
			typeIxMap.put(COLS[i], i);
		}
	}
	
	private Types getType(String s) {
		return  Types.valueOf(s);
	}
	

	public boolean next() throws IOException{
		if(reader != null){
			currentRow  = reader.readLine();
			if(currentRow == null){
				return false;
			}

			lineNumber++;

			if (this.currentRow.trim().length() == 0) {
				return next();
			}
			handler.process(this.currentRow.split("\\t", -1));
			return true;
		}
		return false;
	}
	
	public boolean process(String[] colVals) {
		for (int i : colIxSubset) {
			String v = colVals[i];
			String[] vals = v.split("; ",-1);
		}
		return true;
	}
	
	public void parse(Reader reader) throws IOException{
		init();
		handler.typeMap = typeIxMap;
		handler.init();
	
		this.reader = new BufferedReader(reader);
		while (next()) {
			
		}
	}

	/**
	 * 
	 * @param file is the location of the PIR file. The location
	 *  could be http url, absolute path and uri. The could refer to a gaf file or compressed gaf (gzip fle).
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void parse(String file) throws IOException, URISyntaxException{
		// TODO - abstract common code with GAFParser into abstract class
		if(file == null){
			throw new IOException("File '" + file + "' file not found");
		}
		
		InputStream is = null;
		
		if(file.startsWith("http://")){
			URL url = new URL(file);
			is = url.openStream();
		}else if(file.startsWith("file:/")){
			is = new FileInputStream(new File(new URI(file)));
		}else{
			is = new FileInputStream(file);
		}
		
		if(file.endsWith(".gz")){
			is = new GZIPInputStream(is);
		}
		
		parse(new InputStreamReader(is));
		
	}

	
	public void parse(File gaf_file)
			throws IOException {


		// String message = "Importing GAF data";
		try {
			parse(gaf_file.getAbsoluteFile().toString());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

	
}

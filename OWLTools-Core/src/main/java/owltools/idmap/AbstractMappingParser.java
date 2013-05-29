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
import java.util.zip.GZIPInputStream;

/**
 * Parses the IDMapping file available here:
 * ftp://ftp.pir.georgetown.edu/databases/idmapping/idmapping.tb.gz
 * 
 * @author cjm
 * 
 */
public abstract class AbstractMappingParser {
	

	protected BufferedReader reader;
	
	protected String currentRow;
	
	protected String currentCols[];
	
	protected int expectedNumCols;
	
	protected int lineNumber;

	/**
	 * 
	 * @param file is the location of the mapping file. The location
	 *  could be http url, absolute path and uri. 
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

	
	public abstract void parse(Reader reader) throws IOException;


	public void parse(File file)
			throws IOException {


		// String message = "Importing GAF data";
		try {
			parse(file.getAbsoluteFile().toString());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

	
}

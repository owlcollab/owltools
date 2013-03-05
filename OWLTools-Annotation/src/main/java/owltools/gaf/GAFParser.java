package owltools.gaf;

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
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * 
 * @author Shahid Manzoor
 * 
 */
public class GAFParser {

	private static final String GAF_COMMENT = "!";
	private static final String GAF_VERSION = GAF_COMMENT + "gaf-version:";
	protected static Logger LOG = Logger.getLogger(GAFParser.class);
	private static boolean DEBUG = LOG.isDebugEnabled();

	public final static int DB = 0;
	public final static int DB_OBJECT_ID = 1;
	public final static int DB_OBJECT_SYMBOL = 2;
	public final static int QUALIFIER = 3;
	public final static int GOID = 4;
	public final static int REFERENCE = 5;
	public final static int EVIDENCE = 6;
	public final static int WITH = 7;
	public final static int ASPECT = 8;
	public final static int DB_OBJECT_NAME = 9;
	public final static int DB_OBJECT_SYNONYM = 10;
	public final static int DB_OBJECT_TYPE = 11;
	public final static int TAXON = 12;
	public final static int DATE = 13;
	public final static int ASSIGNED_BY = 14;
	public final static int ANNOTATION_XP = 15;
	public final static int GENE_PRODUCT_ISOFORM = 16;

	
	private double gafVersion;
	
	private BufferedReader reader;
	
	private String currentRow;
	
	private String currentCols[];
	
	private int expectedNumCols;
	
	private int lineNumber;
	
	private List<Object> voilations;
	
	private List<GafParserListener> parserListeners;
	
	
	public List<Object> getAnnotationRuleViolations(){
		return this.voilations;
	}
	
	/**
	 * Try to parse a next line, may skip lines until a line is parsed successfully or the file ends.
	 * 
	 * @return true, if there is a next line.
	 * @throws IOException
	 */
	public boolean next() throws IOException{
		
		while (true) {
			ReadState state = loadNext();
			if (state == ReadState.success) {
				return true;
			}
			else if (state == ReadState.no) {
				return false;
			}
			// default ReadState.next
			// continue loop
		}
	}
	
	/**
	 * Using a recursive call to check if a next line exists, may lead to an over flow.
	 * Use a while loop with a function call, which has a tri-state return value.
	 * 
	 * @return ReadState
	 * @throws IOException
	 */
	private ReadState loadNext() throws IOException{
		if(reader != null){
			currentRow  = reader.readLine();
			if(currentRow == null){
				return ReadState.no;
			}

			lineNumber++;
			if(DEBUG)
				LOG.debug("Parsing Row: " +lineNumber + " -- " +currentRow);

			final String trimmedLine = StringUtils.trimToEmpty(currentRow);
			if (trimmedLine.length() == 0) {
				LOG.warn("Blank Line");
				return ReadState.next;
			}else if (trimmedLine.startsWith(GAF_COMMENT)) {
				
				if(gafVersion<1){
				
					if (isFormatDeclaration(trimmedLine)) {
						gafVersion = parseGafVersion(trimmedLine);
						if (gafVersion == 2.0) {
							expectedNumCols = 17;
						}
					}
				}
				return ReadState.next;
			}else{
				
				
				fireParsing();
				
				// use more efficient implementation to split line
				// this.currentRow.split("\\t", -1);
				this.currentCols = StringUtils.splitPreserveAllTokens(this.currentRow, '\t');
				if (expectedNumCols == 17 && currentCols.length == 16) {
					LOG.warn("Fix missing tab for GAF 2.0 format in line: "+lineNumber);
					// repair
					// add an empty "" to the array
					this.currentCols = Arrays.copyOf(currentCols, 17);
					this.currentCols[16] = "";
					fireParsingWarning("Fix missing tab for GAF 2.0 format, expected 17 columns but found only 16.");
				}
				if (expectedNumCols == 17 && currentCols.length == 15) {
					LOG.warn("Fix missing tabs for GAF 2.0 format in line: "+lineNumber);
					// repair
					// add two empty "" to the array
					this.currentCols = Arrays.copyOf(currentCols, 17);
					this.currentCols[15] = "";
					this.currentCols[16] = "";
					fireParsingWarning("Fix missing tab for GAF 2.0 format, expected 17 columns but found only 15.");
				}
				if (currentCols.length != expectedNumCols) {

					String error = "Got invalid number of columns for row (expected "
						+ expectedNumCols
						+ ", got "
						+ currentCols.length
						+ "). The '"+lineNumber+"' row is ignored.";
	
					if(currentCols.length<expectedNumCols){
						String v =error;
						voilations.add(v);
						fireParsingError(error);
						LOG.error(error + " : " + this.currentRow);
						return ReadState.next;
					}else{
						fireParsingWarning(error);
						LOG.warn(error + " : " + this.currentRow);
					}
				}
				return ReadState.success;
			}
			
		}
		
		return ReadState.no;
			
	}
	
	private enum ReadState {
		success,
		no,
		next
	}
	
	private void fireParsing(){
		for(GafParserListener listner: parserListeners){
			listner.parsing(this.currentRow, lineNumber);
		}
	}
	
	
	private void fireParsingError(String message){
		for(GafParserListener listner: parserListeners){
			listner.parserError(message, this.currentRow, lineNumber);
		}
	}
	
	private void fireParsingWarning(String message){
		for(GafParserListener listner: parserListeners){
			if (listner.reportWarnings()) {
				listner.parserWarning(message, this.currentRow, lineNumber);
			}
		}
	}
	
	public String getCurrentRow(){
		return this.currentRow;
	}
	
	public GAFParser(){
		init();
		
	}
	
	public void init(){
		this.gafVersion = 0;
		this.reader = null;
		this.expectedNumCols = 15;
		voilations = new Vector<Object>();
		lineNumber = 0;
		if(parserListeners == null){
			parserListeners = new Vector<GafParserListener>();
		}
	}
	
	public void addParserListener(GafParserListener listener){
		if(listener == null)
			return;
		
		if(!parserListeners.contains(listener))
			parserListeners.add(listener);
	}
	
	public void remoteParserListener(GafParserListener listener){
		if(listener == null)
			return;
		
		parserListeners.remove(listener);
	}

	
	public void parse(Reader reader){
		init();
		if (DEBUG)
			LOG.debug("Parsing Start");
	
		this.reader = new BufferedReader(reader);
	}

	/**
	 * 
	 * @param file is the location of the gaf file. The location
	 *  could be http url, absolute path and uri. The could refer to a gaf file or compressed gaf (gzip fle).
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void parse(String file) throws IOException, URISyntaxException{
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
	
	private void checkNext(){
		if(this.currentCols == null){
			throw new IllegalStateException("Error occured becuase either there is no further " +
					"record in the file or parse method is not called yet");
		}
	}
	
	// Col. 1
	public String getDb(){
		checkNext();
		
		return this.currentCols[DB];
	}
	
	// Col. 2
	public String  getDbObjectId(){
		checkNext();
		
		return this.currentCols[DB_OBJECT_ID];
	}
	
	// Col. 3
	public String  getDbObjectSymbol(){
		checkNext();
		
		return this.currentCols[DB_OBJECT_SYMBOL];
	}
	
	// Col. 4
	public String  getQualifier(){
		checkNext();
		
		return this.currentCols[QUALIFIER];
	}

	// Col. 5
	public String  getGOId(){
		checkNext();
		
		return this.currentCols[GOID];
	}
	
	// Col. 6
	public String  getReference(){
		checkNext();
		
		return this.currentCols[REFERENCE];
		
	}
	
	// Col. 7
	public String  getEvidence(){
	
		checkNext();
		
		return this.currentCols[EVIDENCE];
		
	}
	
	// Col. 8
	public String  getWith(){
		checkNext();
		
		return this.currentCols[WITH];

	}
	
	// Col. 9
	public String  getAspect(){
		checkNext();
		
		return this.currentCols[ASPECT];

	}
	
	// Col. 10
	public String  getDbObjectName(){
		checkNext();
		
		String s = this.currentCols[DB_OBJECT_NAME];
		s = s.replace("\\", "\\\\");
		
		return s;

	}
	
	// Col. 11
	public String  getDbObjectSynonym(){
		checkNext();
		
		return this.currentCols[DB_OBJECT_SYNONYM];

	}
	
	// Col. 12
	public String  getDBObjectType(){
		checkNext();
		
		return this.currentCols[DB_OBJECT_TYPE];

	}
	
	// Col. 13
	public String  getTaxon(){
		checkNext();
		
		return this.currentCols[TAXON];

	}
	
	// Col. 14
	public String  getDate(){
		checkNext();
		
		return this.currentCols[DATE];

	}
	
	// Col. 15
	public String  getAssignedBy(){
		checkNext();
		
		return this.currentCols[ASSIGNED_BY];

	}
	
	// Col. 16
	public String  getAnnotationExtension(){
		checkNext();

		if(this.currentCols.length>15){
			return this.currentCols[ANNOTATION_XP];
		}
		
		return null;
		
	}
	
	// Col. 17
	public String  getGeneProjectFormId(){
		checkNext();

		if(this.currentCols.length>16){
			return this.currentCols[GENE_PRODUCT_ISOFORM];
		}
		
		return null;
	}
	

	private boolean isFormatDeclaration(String line) {
		return line.startsWith(GAF_VERSION);
	}

	private double parseGafVersion(String line) {
		Pattern p = Pattern.compile(GAF_VERSION + "\\s*(\\d+\\.*\\d+)");
		Matcher m = p.matcher(line);
		if (m.matches()) {
			return Double.parseDouble(m.group(1));
		}
		return 0;
	}

	public int getLineNumber() {
		return lineNumber;
	}
	
}

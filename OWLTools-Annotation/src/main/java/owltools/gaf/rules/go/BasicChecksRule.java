package owltools.gaf.rules.go;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

import owltools.gaf.ExtensionExpression;
import owltools.gaf.GAFParser;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;

/**
 * This class performs basic checks. See the GO_AR:0000001 rule in the 
 * {@link "http://www.geneontology.org/quality_control/annotation_checks/annotation_qc.xml"} 
 * file for details about the checks.
 * 
 * @author Shahid Manzoor
 */
public class BasicChecksRule extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.gold.rules.BasicChecksRule";

	private static Logger LOG = Logger.getLogger(BasicChecksRule.class);
	
	public static final ThreadLocal<SimpleDateFormat> dtFormat = new ThreadLocal<SimpleDateFormat>(){

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd");
		}
	};
	
	private final Set<String> db_abbreviations;
	private final Set<String> ieaCodes;
	
	/**
	 * @param xrefAbbsLocation
	 * @param eco
	 */
	public BasicChecksRule(String xrefAbbsLocation, TraversingEcoMapper eco) {
		db_abbreviations = buildAbbreviations(xrefAbbsLocation);
		ieaCodes = eco.getAllValidEvidenceIds("IEA", true);
	}
	
	private static Set<String> buildAbbreviations(String path){
		Set<String> set = new HashSet<String>();
		
		InputStream is = null;
		LineIterator it = null;
		try{
			if(path.startsWith("http://") || path.startsWith("file:/")){
				is = new URL(path).openStream();
			}
			else {
				is = new FileInputStream(new File(path));
			}
			
			it = new LineIterator(new InputStreamReader(is));
			
			while (it.hasNext()) {
				String line = it.next();
				
				if(line.startsWith("!"))
					continue;
					
				String data[] = line.split(":");
				String tag = data[0].trim();
				if(data.length==2 && ("abbreviation".equals(tag) || "synonym".equals(tag) ) ){
					set.add(data[1].trim());
				}
			}			
		}catch(Exception ex){
			LOG.error("Can't read xref abbs file at the location: " + path, ex);
		}
		finally {
			IOUtils.closeQuietly(is);
			LineIterator.closeQuietly(it);
		}
		
		return set;
		
	}
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {

		HashSet<AnnotationRuleViolation> set = new HashSet<AnnotationRuleViolation>();
		
		String row = a.toString();
		
		String cols[] = row.split("\\t", -1);
		//cardinality checks
		checkCardinality(cols[0],0, "Column 1: DB", row,1,1, set,a);
		checkCardinality(cols[1], 1,"Column 2: DB Object ID", row,1,1, set,a);
		checkCardinality(cols[2], 2,"Column 3: DB Object Symbol", row,1,1, set,a);
		checkCardinality(cols[3], 3,"Column 4: Qualifier", row, 0, 3, set,a);
		checkCardinality(cols[4], 4,"Column 5: GO ID", row,1,1, set,a);
		checkCardinality(cols[5], 5,"Column 6: DB Reference", row,1,3, set,a);
		checkCardinality(cols[6], 6,"Column 7: Evidence Code", row,1,3, set,a);
		checkCardinality(cols[7], 7,"Column 8: With or From", row,0,3, set,a);
	//	checkCardinality(cols[8], 8,"Column 9: Aspect", row,1,1, set);
		checkCardinality(cols[9], 9,"Column 10: DB Object Name", row,0,1, set,a);
		checkCardinality(cols[10], 10,"Column 11: DB Object Synonym",  row, 0,3, set,a);
		checkCardinality(cols[11], 11,"Column 12: DB Object Type", row, 1,1, set,a);
		checkCardinality(cols[12], 12,"Column 13: Taxon", row, 1,2, set,a);
		checkCardinality(cols[13], 13,"Column 14: Date", row, 1,1, set,a);
		checkCardinality(cols[14], 14,"Column 15: DB Object Type", row, 1,1, set,a);
		
		if(cols.length>15){
			checkCardinality(cols[15], 15,"Column 16: DB Object Type", row, 0,3, set,a);
			if (cols.length>16) {
				// check otherwise, there is an un-informative array-out-of-bound exception for an optional value
				checkCardinality(cols[16], 16,"Column 17: DB Object Type", row, 0,3, set,a);
			}
		}
		
		
		//check date format
		String dtString = cols[GAFParser.DATE];
		try{
			// check that the date parses
			Date annotationDate = dtFormat.get().parse(dtString);
			
			// check that IEA annotations are not older than one year
			if (ieaCodes.contains(a.getEvidenceCls())) {
				Calendar todayMinusOneYear = Calendar.getInstance();  
				todayMinusOneYear.add(Calendar.YEAR, -1);
				Date time = todayMinusOneYear.getTime();
				if (annotationDate.before(time)) {
					AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "IEA evidence code present with a date more than a year old '"+dtString+"'" , a, ViolationType.Error);
					set.add(v);
				}
			}
		}catch(Exception ex){
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "The date in the column 14 is of incorrect format in the row: " , a);
			set.add(v);
		}
		
		//taxon check
		String[] taxons  = cols[GAFParser.TAXON].split("\\|");
		checkTaxon(taxons[0], row, set,a);
		if(taxons.length>1){
			checkTaxon(taxons[1], row, set,a);
		}
		
		//check db abbreviations
		if(!db_abbreviations.contains(cols[0])){
			AnnotationRuleViolation v= new AnnotationRuleViolation(getRuleId(), "The DB '" + cols[0] + "'  referred in the column 1 is incorrect in the row: " , a);
			set.add(v);
		}
		
		// check that in c16 all IDs are prefixed.
		List<List<ExtensionExpression>> groupedExpressions = a.getExtensionExpressions();
		if (groupedExpressions != null && !groupedExpressions.isEmpty()) {
			for (List<ExtensionExpression> expressions : groupedExpressions) {
				if (expressions != null && !expressions.isEmpty()) {
					for (ExtensionExpression extensionExpression : expressions) {
						String cls = extensionExpression.getCls();
						int dbSepPos = cls.indexOf(':');
						if (dbSepPos <= 0) {
							AnnotationRuleViolation v= new AnnotationRuleViolation(getRuleId(), "All identifiers in column 16 need a prefix. The id '" + cls + "' has no prefix. " , a);
							set.add(v);
						}
					}
				}
			}
		}
		return set;
	}


	
	
	private void checkCardinality(String value,int col, String columnName, String row, int min, int max, HashSet<AnnotationRuleViolation> voilations, GeneAnnotation a){

		//TODO: check white spaces
		/*if(value != null && value.length() != value.trim().length()){
			voilations.add(new AnnotationRuleViolation("White Spaces are found in the " + columnName+ " column in the row: " + row));
		}*/

		/*if(min==0 && value != null && value.length() != value.trim().length()){
			voilations.add(new AnnotationRuleViolation("White Spaces are found in the " + columnName+ " column in the row: " + row));
		}*/
		
		if(min>=1 && value.length()==0){
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), columnName +" value is not supplied in the row: " ,a );
			voilations.add(v);
		}
		
		if(max==1 && value.contains("|")){
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), columnName +" cardinality is found greater than 1 in the row: " ,a);
			voilations.add(v);
		}
		
		
		if(value != null){
			String tokens[] = value.split("\\|");
			
			if(max==2 && tokens.length>2){
				AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), columnName +" cardinality is found greater than 2 in the row: " ,a);
				voilations.add(v);
			}
			
			if(tokens.length>1){
				for(int i =1;i<tokens.length;i++){
					String token = tokens[i]; 
					checkWhiteSpaces(token, col, columnName, row, voilations,a);
				}
			}
		}
	}
	
	private void checkWhiteSpaces(String value,int col, String columnName, String row, HashSet<AnnotationRuleViolation> voilations, GeneAnnotation a){

		if(col == GAFParser.DB_OBJECT_NAME || col == GAFParser.DB_OBJECT_SYNONYM || col == GAFParser.DB_OBJECT_SYMBOL)
			return;
		
		if(value.contains(" ")){
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "White Spaces are found in the " + columnName+ " column in the row: " ,a);
			voilations.add(v);
		}
	}
	
	private void checkTaxon(String value, String row, HashSet<AnnotationRuleViolation> voilations, GeneAnnotation a){
		if(!value.startsWith("taxon")){
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "The taxon id in the column 13 is of in correct format in the row :" ,a);
			voilations.add(v);
		}
		
		try{
			String taxon = value.substring("taxon:".length());
			Integer.parseInt(taxon);
		}catch(Exception ex){
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "The taxon id in the column 13 is not an integer value :" ,a);
			voilations.add(v);
		}
	}
	
	
	/*
	private void checkWhiteSpaces(String value, String columnName, HashSet<AnnotationRuleViolation> set, GeneAnnotation a){

		if(value.length() != value.trim().length()){
			set.add(new AnnotationRuleViolation(getRuleId(), "Spaces are not allowed in the " + columnName+ " column", a));
		}
		
	}
	
	private void checkCardinality(String value, String columnName, HashSet<AnnotationRuleViolation> set, GeneAnnotation a, int min, int max){

		if(min>0 && value.length() != value.trim().length()){
			set.add(new AnnotationRuleViolation(getRuleId(), "Spaces are not allowed in the " + columnName+ " column", a));
		}

		if(min==0 && value != null && value.length() != value.trim().length()){
			set.add(new AnnotationRuleViolation(getRuleId(), "Spaces are not allowed in the " + columnName+ " column", a));
		}
		
		if(min>=1 && value.length()==0){
			set.add(new AnnotationRuleViolation(getRuleId(), columnName +" column cannot be empty", a));
		}
		
		if(max==1 && value.contains("|")){
			set.add(new AnnotationRuleViolation(getRuleId(), columnName +" colmn cardinality cannt be greater than 1", a));
		}
		
		String tokens[] = value.split("|");
		
		if(tokens.length>1){
			for(String token: tokens){
				checkWhiteSpaces(token, columnName, set, a);
			}
		}
		
	}*/
	
	
}

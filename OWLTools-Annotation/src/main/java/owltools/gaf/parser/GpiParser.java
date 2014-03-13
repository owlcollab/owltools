package owltools.gaf.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class GpiParser extends AbstractAnnotationFileParser {
	
	private static final Logger LOG = Logger.getLogger(GpiParser.class);
	
	private static final String COMMENT_PREFIX = "!";
	private static final String VERSION_PREFIX = "gpi-version:";
	private static final double DEFAULT_VERSION = 0.0d;
	private static final int EXPECTED_COLUMNS = 9;

	
	public GpiParser() {
		super(DEFAULT_VERSION, COMMENT_PREFIX, "gpi");
	}

	static enum GpiColumns {
		
		DB_Object_ID(1, "DB_Object_ID", true),
		DB_Object_Symbol(2, "DB_Object_Symbol", true),
		DB_Object_Name(3, "DB_Object_Name", false),
		DB_Object_Synonym(4, "DB_Object_Synonym", false),
		DB_Object_Type(5, "DB_Object_Type", true),
		Taxon(6, "Taxon", true),
		Parent_Object_ID(7, "Parent_Object_ID", false),
		DB_Xref(8, "DB_Xref", false),
		Gene_Product_Properties(9, "Gene_Product_Properties", false);
		
		private final int pos;
		
		private GpiColumns(int pos, String name, boolean required) {
			this.pos = pos;
		}
		
		private int index() {
			return pos - 1;
		}
	}
	
	public String getColumn(GpiColumns col) {
		return currentCols[col.index()];
	}
	
	public String getDB_Object_ID() {
		return currentCols[GpiColumns.DB_Object_ID.index()];
	}
	
	public String getDB_Object_Symbol() {
		return currentCols[GpiColumns.DB_Object_Symbol.index()];
	}
	
	public String getDB_Object_Name() {
		return currentCols[GpiColumns.DB_Object_Name.index()];
	}
	
	public String getDB_Object_Synonym() {
		return currentCols[GpiColumns.DB_Object_Synonym.index()];
	}
	
	public String getDB_Object_Type() {
		return currentCols[GpiColumns.DB_Object_Type.index()];
	}
	
	public String getTaxon() {
		return currentCols[GpiColumns.Taxon.index()];
	}
	
	public String getParent_Object_ID() {
		return currentCols[GpiColumns.Parent_Object_ID.index()];
	}
	
	public String getDB_Xref() {
		return currentCols[GpiColumns.DB_Xref.index()];
	}
	
	public String getGene_Product_Properties() {
		return currentCols[GpiColumns.Gene_Product_Properties.index()];
	}
	
	//----------------------------
	//
	//----------------------------
	
	@Override
	protected boolean isFormatDeclaration(String line) {
		return line.startsWith(COMMENT_PREFIX+VERSION_PREFIX);
	}
	
	@Override
	protected double parseVersion(String line) {
		String versionString = line.substring(COMMENT_PREFIX.length()+VERSION_PREFIX.length());
		versionString = StringUtils.trimToNull(versionString);
		if (versionString != null) {
			try {
				return Double.parseDouble(versionString);
			} catch (NumberFormatException e) {
				LOG.info("Could not parse version from line: "+line);
			}
		}
		// fallback: return defaultVersion
		return DEFAULT_VERSION;
	}

	@Override
	protected int getExpectedColumnCount() {
		return EXPECTED_COLUMNS;
	}
	
}

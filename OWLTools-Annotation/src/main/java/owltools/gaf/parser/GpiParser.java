package owltools.gaf.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class GpiParser extends AbstractAnnotationFileParser {
	
	private static final Logger LOG = Logger.getLogger(GpiParser.class);
	
	private static final String COMMENT_PREFIX = "!";
	private static final String VERSION_PREFIX = "gpi-version:";
	private static final String NAMESPACE_PREFIX = "namespace:";
	private static final double DEFAULT_VERSION = 1.1d;
	private static final int EXPECTED_COLUMNS_V11 = 9;
	private static final int EXPECTED_COLUMNS_V12 = 10;

	private String namespace = null;
	
	public GpiParser() {
		super(DEFAULT_VERSION, COMMENT_PREFIX, "gpi");
	}

	static enum GpiColumns {
		
		DB(1, "DB", true, 1.2),
		DB_Object_ID(2, "DB_Object_ID", true, 1.1),
		DB_Object_Symbol(3, "DB_Object_Symbol", true, 1.1),
		DB_Object_Name(4, "DB_Object_Name", false, 1.1),
		DB_Object_Synonym(5, "DB_Object_Synonym", false, 1.1),
		DB_Object_Type(6, "DB_Object_Type", true, 1.1),
		Taxon(7, "Taxon", true, 1.1),
		Parent_Object_ID(8, "Parent_Object_ID", false, 1.1),
		DB_Xref(9, "DB_Xref", false, 1.1),
		Gene_Product_Properties(10, "Gene_Product_Properties", false, 1.1);
		
		private final int pos;
		
		private GpiColumns(int pos, String name, boolean required, double since) {
			this.pos = pos;
		}
		
		private int index(double version) {
			if (version < 1.2d) {
				return pos - 2;
			}
			else {
				return pos - 1;
			}
		}
	}
	
	public String getColumn(GpiColumns col) {
		return currentCols[col.index(version)];
	}
	
	public String getDB_Object_ID() {
		return currentCols[GpiColumns.DB_Object_ID.index(version)];
	}
	
	public String getDB_Object_Symbol() {
		return currentCols[GpiColumns.DB_Object_Symbol.index(version)];
	}
	
	public String getDB_Object_Name() {
		return currentCols[GpiColumns.DB_Object_Name.index(version)];
	}
	
	public String getDB_Object_Synonym() {
		return currentCols[GpiColumns.DB_Object_Synonym.index(version)];
	}
	
	public String getDB_Object_Type() {
		return currentCols[GpiColumns.DB_Object_Type.index(version)];
	}
	
	public String getTaxon() {
		return currentCols[GpiColumns.Taxon.index(version)];
	}
	
	public String getParent_Object_ID() {
		return currentCols[GpiColumns.Parent_Object_ID.index(version)];
	}
	
	public String getDB_Xref() {
		return currentCols[GpiColumns.DB_Xref.index(version)];
	}
	
	public String getGene_Product_Properties() {
		return currentCols[GpiColumns.Gene_Product_Properties.index(version)];
	}
	
	public String getNamespace() {
		if (version >= 1.2d) {
			return currentCols[GpiColumns.DB.index(version)];
		}
		return namespace;
	}
	
	//----------------------------
	//
	//----------------------------
	
	@Override
	protected void handleComment(final String line) {
		if(version < 1.0){
			if (isFormatDeclaration(line)) {
				version = parseVersion(line);
			}
		}
		else if (namespace == null) {
			if (line.startsWith(COMMENT_PREFIX+NAMESPACE_PREFIX)) {
				namespace = line.substring(COMMENT_PREFIX.length()+NAMESPACE_PREFIX.length());
				namespace = StringUtils.trimToNull(namespace);
			}
		}
	}
	
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
		if (version < 1.2) {
			return EXPECTED_COLUMNS_V11;
		}
		return EXPECTED_COLUMNS_V12;
	}
	
}

package owltools.gaf.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class GpadParser extends AbstractAnnotationFileParser {
	
	private static final Logger LOG = Logger.getLogger(GpadParser.class);

	private static final String COMMENT_PREFIX = "!";
	private static final String VERSION_STRING_SHORT = "gpa-version";
	private static final String VERSION_STRING_LONG = "gpad-version";
	private static final double DEFAULT_VERSION = 1.1d;
	private static final int EXPECTED_COLUMNS = 12;
	
	
	public GpadParser() {
		super(DEFAULT_VERSION, COMMENT_PREFIX, "gpad");
	}
	
	static enum GpadColumns {
		
		DB(1, "DB", true),
		DB_Object_ID(2, "DB_Object_ID", true),
		Qualifier(3, "Qualifier", true),
		GO_ID(4, "GO ID", true),
		DB_Reference(5, "DB:Reference(s)", true),
		Evidence_Code(6, "Evidence code", true),
		With(7, "With (or) From", false),
		Interacting_Taxon_ID(8, "Interacting taxon ID", false),
		Date(9, "Date", true),
		Assigned_by(10, "Assigned_by", true),
		Annotation_Extension(11, "Annotation Extension", true),
		Annotation_Properties(12, "Annotation Properties", true);
		
		private final int pos;
		
		private GpadColumns(int pos, String name, boolean required) {
			this.pos = pos;
		}
		
		private int index(double version) {
			return pos - 1;
		}
	}
	
	public String getColumn(GpadColumns col) {
		return currentCols[col.index(version)];
	}
	
	public String getDB() {
		return currentCols[GpadColumns.DB.index(version)];
	}

	public String getDB_Object_ID() {
		return currentCols[GpadColumns.DB_Object_ID.index(version)];
	}

	public String getQualifier() {
		return currentCols[GpadColumns.Qualifier.index(version)];
	}

	public String getGO_ID() {
		return currentCols[GpadColumns.GO_ID.index(version)];
	}

	public String getDB_Reference() {
		return currentCols[GpadColumns.DB_Reference.index(version)];
	}

	public String getEvidence_Code() {
		return currentCols[GpadColumns.Evidence_Code.index(version)];
	}

	public String getWith() {
		return currentCols[GpadColumns.With.index(version)];
	}

	public String getInteracting_Taxon_ID() {
		return currentCols[GpadColumns.Interacting_Taxon_ID.index(version)];
	}

	public String getDate() {
		return currentCols[GpadColumns.Date.index(version)];
	}

	public String getAssigned_by() {
		return currentCols[GpadColumns.Assigned_by.index(version)];
	}

	public String getAnnotation_Extension() {
		return currentCols[GpadColumns.Annotation_Extension.index(version)];
	}

	public String getAnnotation_Properties() {
		return currentCols[GpadColumns.Annotation_Properties.index(version)];
	}
	
	//----------------------------
	//
	//----------------------------

	@Override
	protected boolean isFormatDeclaration(String line) {
		if (line.startsWith(COMMENT_PREFIX)) {
			line = line.substring(1);
			line = StringUtils.trimToEmpty(line);
			if (line.startsWith(VERSION_STRING_LONG)) {
				line = line.substring(VERSION_STRING_LONG.length());
			}
			else if (line.startsWith(VERSION_STRING_SHORT)) {
				line = line.substring(VERSION_STRING_SHORT.length());
			}
			else {
				return false;
			}
			line = StringUtils.trimToEmpty(line);
			if (line.startsWith(":")) {
				line = line.substring(1);
				line = StringUtils.trimToEmpty(line);
				if (line.length() > 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	protected boolean isHeaderMetaData(String line) {
		return isFormatDeclaration(line);
	}

	@Override
	protected double parseVersion(String line) {
		String versionString = null;
		int pos = line.indexOf(':');
		if (pos > 0) {
			versionString = line.substring(pos + 1);
			versionString = StringUtils.trimToNull(versionString);
			if (versionString != null) {
				try {
					return Double.parseDouble(versionString);
				} catch (NumberFormatException e) {
					LOG.info("Could not parse version from line: "+line);
				}
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

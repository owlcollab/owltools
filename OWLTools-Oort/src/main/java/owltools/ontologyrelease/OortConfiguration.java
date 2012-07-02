package owltools.ontologyrelease;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.obolibrary.owl.LabelFunctionalFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

import owltools.InferenceBuilder;

/**
 * Parameters for {@link OboOntologyReleaseRunner}. Contains methods 
 * to read and write the configuration from {@link Properties}.
 */
public class OortConfiguration {

	private static final Logger LOGGER = Logger.getLogger(OortConfiguration.class);
	
	public enum MacroStrategy {
		GCI, INPLACE 
	}
	
	private Vector<String> paths = new Vector<String>();
	private File base = new File(".");

	private String reasonerName = InferenceBuilder.REASONER_HERMIT;
	private boolean enforceEL = false;
	private boolean writeELOntology = false;
	private boolean asserted = false;
	private boolean simple = false;
	private boolean allowFileOverWrite = false;
	private boolean expandXrefs = false;
	private boolean recreateMireot = true;
	private boolean repairAnnotationCardinality = false;
	private boolean expandShortcutRelations = false;
	private boolean allowEquivalentNamedClassPairs = false;
	private MacroStrategy macroStrategy = MacroStrategy.GCI;
	private boolean checkConsistency = true;
	private boolean writeMetadata = true;
	private boolean writeSubsets = true;
	private boolean justifyAssertedSubclasses = false;
	private String justifyAssertedSubclassesFrom = null;
	private Set<String> sourceOntologyPrefixes = null;
	private boolean executeOntologyChecks = true;
	private boolean forceRelease = false;
	private boolean ignoreLockFile = false;
	private boolean autoDetectBridgingOntology = true;
	private boolean removeDanglingBeforeReasoning = false;
	private boolean addSupportFromImports = false;
	private boolean addImportsFromSupports = false;
	private boolean translateDisjointsToEquivalents = false;
	private List<String> bridgeOntologies = new ArrayList<String>();
	private List<String> toBeMergedOntologies = new ArrayList<String>(); // TODO
	private Set<PropertyView> propertyViews = new HashSet<PropertyView>();
	private boolean useReleaseFolder = true;
	private Set<String> skipFormatSet = new HashSet<String>();
	private boolean gafToOwl = false;
	private String catalogXML = null;
	
	private boolean writeLabelOWL = false;
	
	private boolean useQueryOntology = false;
	private String queryOntology = null; 
	private String queryOntologyReference = null;
	private boolean queryOntologyReferenceIsIRI = true;
	private boolean removeQueryOntologyReference = false;

	private OWLOntologyFormat defaultFormat = new RDFXMLOntologyFormat();
	private OWLOntologyFormat owlXMLFormat = new OWLXMLOntologyFormat();
	private static final OWLOntologyFormat owlOFNFormat = new LabelFunctionalFormat(); 

	/**
	 * @return the reasoner name
	 */
	public String getReasonerName() {
		return reasonerName;
	}

	/**
	 * Set the reasoner name
	 * 
	 * @param reasonerName
	 */
	public void setReasonerName(String reasonerName) {
		this.reasonerName = reasonerName;
	}

	/**
	 * @return is asserted
	 */
	public boolean isAsserted() {
		return asserted;
	}

	/**
	 * Set asserted
	 * 
	 * @param asserted
	 */
	public void setAsserted(boolean asserted) {
		this.asserted = asserted;
	}

	/**
	 * @return is simple
	 */
	public boolean isSimple() {
		return simple;
	}

	/**
	 * Set simple.
	 * 
	 * @param simple
	 */
	public void setSimple(boolean simple) {
		this.simple = simple;
	}

	/**
	 * @return is expand Xrefs
	 */
	public boolean isExpandXrefs() {
		return expandXrefs;
	}

	/**
	 * Set export bridges
	 * 
	 * @param expandXrefs
	 */
	public void setExpandXrefs(boolean expandXrefs) {
		this.expandXrefs = expandXrefs;
	}

	/**
	 * @return is AllowFileOverWrite
	 */
	public boolean isAllowFileOverWrite() {
		return allowFileOverWrite;
	}

	/**
	 * Set allowFileOverWrite 
	 * 
	 * @param allowFileOverWrite
	 */
	public void setAllowFileOverWrite(boolean allowFileOverWrite) {
		this.allowFileOverWrite = allowFileOverWrite;
	}

	/**
	 * @return {@link MacroStrategy}
	 */
	public MacroStrategy getMacroStrategy() {
		return macroStrategy;
	}

	/**
	 * Set the {@link MacroStrategy}
	 * 
	 * @param macroStrategy
	 */
	public void setMacroStrategy(MacroStrategy macroStrategy) {
		this.macroStrategy = macroStrategy;
	}

	/**
	 * @return is recreateMireot
	 */
	public boolean isRecreateMireot() {
		return recreateMireot;
	}

	/**
	 * Set recreateMireot
	 * 
	 * @param recreateMireot
	 */
	public void setRecreateMireot(boolean recreateMireot) {
		this.recreateMireot = recreateMireot;
	}

	/**
	 * @return is ExpandShortcutRelations
	 */
	public boolean isExpandShortcutRelations() {
		return expandShortcutRelations;
	}

	public void setExpandShortcutRelations(boolean expandShortcutRelations) {
		this.expandShortcutRelations = expandShortcutRelations;
	}

	public boolean isEnforceEL() {
		return enforceEL;
	}

	public void setEnforceEL(boolean enforceEL) {
		this.enforceEL = enforceEL;
	}

	public boolean isWriteELOntology() {
		return writeELOntology;
	}

	public void setWriteELOntology(boolean writeELOntology) {
		this.writeELOntology = writeELOntology;
	}
	
	public Set<String> getSkipFormatSet() {
		return skipFormatSet;
	}

	public void setSkipFormatSet(Set<String> skipFormatSet) {
		this.skipFormatSet = skipFormatSet;
	}
	public void addToSkipFormatSet(String fmt) {
		this.skipFormatSet.add(fmt);
	}
	public void removeFromSkipFormatSet(String fmt) {
		this.skipFormatSet.remove(fmt);
	}
	public boolean isSkipFormat(String fmt) {
		return this.skipFormatSet.contains(fmt);
	}

	public Set<String> getSourceOntologyPrefixes() {
		return sourceOntologyPrefixes;
	}

	public void setSourceOntologyPrefixes(Set<String> sourceOntologyPrefixes) {
		this.sourceOntologyPrefixes = sourceOntologyPrefixes;
	}

	public void addSourceOntologyPrefix(String prefix) {
		if (sourceOntologyPrefixes == null) 
			sourceOntologyPrefixes = new HashSet<String>();
		sourceOntologyPrefixes.add(prefix);
	}

	/**
	 * @return the bridgeOntologies
	 */
	public List<String> getBridgeOntologies() {
		return bridgeOntologies;
	}

	/**
	 * @param bridgeOntologies the bridgeOntologies to set
	 */
	public void setBridgeOntologies(List<String> bridgeOntologies) {
		this.bridgeOntologies = bridgeOntologies;
	}
	
	public void addBridgeOntology(String bridgeOntology) {
		if (bridgeOntologies == null) {
			bridgeOntologies = new ArrayList<String>();
		}
		bridgeOntologies.add(bridgeOntology);
	}

	public boolean isExecuteOntologyChecks() {
		return executeOntologyChecks;
	}

	public void setExecuteOntologyChecks(boolean executeOntologyChecks) {
		this.executeOntologyChecks = executeOntologyChecks;
	}

	public boolean isAllowEquivalentNamedClassPairs() {
		return allowEquivalentNamedClassPairs;
	}

	public void setAllowEquivalentNamedClassPairs(
			boolean allowEquivalentNamedClassPairs) {
		this.allowEquivalentNamedClassPairs = allowEquivalentNamedClassPairs;
	}

	public boolean isWriteSubsets() {
		return writeSubsets;
	}

	public void setWriteSubsets(boolean isWriteSubsets) {
		this.writeSubsets = isWriteSubsets;
	}

	public boolean isJustifyAssertedSubclasses() {
		return justifyAssertedSubclasses;
	}

	public void setJustifyAssertedSubclasses(boolean isJustifyAssertedSubclasses) {
		this.justifyAssertedSubclasses = isJustifyAssertedSubclasses;
	}

	public boolean isAutoDetectBridgingOntology() {
		return autoDetectBridgingOntology;
	}

	public void setAutoDetectBridgingOntology(boolean isAutoDetectBridgingOntology) {
		this.autoDetectBridgingOntology = isAutoDetectBridgingOntology;
	}
	
	/**
	 * @return isExpandMacros
	 */
	public boolean isExpandMacros() {
		return expandShortcutRelations;
	}

	/**
	 * @param expandMacros isExpandMacros to set
	 */
	public void setExpandMacros(boolean expandMacros) {
		this.expandShortcutRelations = expandMacros;
	}

	/**
	 * @return the isForceRelease
	 */
	public boolean isForceRelease() {
		return forceRelease;
	}

	/**
	 * @param forceRelease forceRelease to set
	 */
	public void setForceRelease(boolean forceRelease) {
		this.forceRelease = forceRelease;
	}

	/**
	 * @return isCheckConsistency
	 */
	public boolean isCheckConsistency() {
		return checkConsistency;
	}

	/**
	 * @param checkConsistency the checkConsistency to set
	 */
	public void setCheckConsistency(boolean checkConsistency) {
		this.checkConsistency = checkConsistency;
	}
	
	

	public boolean isRemoveDanglingBeforeReasoning() {
		return removeDanglingBeforeReasoning;
	}

	public void setRemoveDanglingBeforeReasoning(
			boolean removeDanglingBeforeReasoning) {
		this.removeDanglingBeforeReasoning = removeDanglingBeforeReasoning;
	}

	public boolean isAddSupportFromImports() {
		return addSupportFromImports;
	}

	public void setAddSupportFromImports(boolean addSupportFromImports) {
		this.addSupportFromImports = addSupportFromImports;
	}

	
	public boolean isAddImportsFromSupports() {
		return addImportsFromSupports;
	}

	public void setAddImportsFromSupports(boolean addImportsFromSupports) {
		this.addImportsFromSupports = addImportsFromSupports;
	}
	
	public boolean isTranslateDisjointsToEquivalents() {
		return translateDisjointsToEquivalents;
	}

	public void setTranslateDisjointsToEquivalents(
			boolean translateDisjointsToEquivalents) {
		this.translateDisjointsToEquivalents = translateDisjointsToEquivalents;
	}

	/**
	 * @return isWriteMetadata
	 */
	public boolean isWriteMetadata() {
		return writeMetadata;
	}

	/**
	 * @param writeMetadata the writeMetadata to set
	 */
	public void setWriteMetadata(boolean writeMetadata) {
		this.writeMetadata = writeMetadata;
	}

	/**
	 * @return the defaultFormat
	 */
	public OWLOntologyFormat getDefaultFormat() {
		return defaultFormat;
	}

	/**
	 * @return the owlXMLFormat
	 */
	public OWLOntologyFormat getOwlXMLFormat() {
		return owlXMLFormat;
	}
	
	/**
	 * @return the paths
	 */
	public Vector<String> getPaths() {
		return paths;
	}

	/**
	 * @param path the path to add to the set
	 */
	public void addPath(String path) {
		paths.add(path);
	}

	/**
	 * @return the base
	 */
	public File getBase() {
		return base;
	}

	/**
	 * @param base the base to set
	 */
	public void setBase(File base) {
		this.base = base;
	}

	/**
	 * @return the repairAnnotationCardinality
	 */
	public boolean isRepairAnnotationCardinality() {
		return repairAnnotationCardinality;
	}

	/**
	 * @param repairAnnotationCardinality the repairAnnotationCardinality to set
	 */
	public void setRepairAnnotationCardinality(boolean repairAnnotationCardinality) {
		this.repairAnnotationCardinality = repairAnnotationCardinality;
	}
	
	

	public Set<PropertyView> getPropertyViews() {
		return propertyViews;
	}

	public void setPropertyViews(Set<PropertyView> propertyViews) {
		this.propertyViews = propertyViews;
	}

	/**
	 * @return the useReleaseFolder
	 */
	public boolean isUseReleaseFolder() {
		return useReleaseFolder;
	}

	/**
	 * @param useReleaseFolder the useReleaseFolder to set
	 */
	public void setUseReleaseFolder(boolean useReleaseFolder) {
		this.useReleaseFolder = useReleaseFolder;
	}

	/**
	 * @return the gafToOwl
	 */
	public boolean isGafToOwl() {
		return gafToOwl;
	}

	/**
	 * @param gafToOwl the gafToOwl to set
	 */
	public void setGafToOwl(boolean gafToOwl) {
		this.gafToOwl = gafToOwl;
	}

	/**
	 * @return the catalogXML
	 */
	public String getCatalogXML() {
		return catalogXML;
	}

	/**
	 * @param catalogXML the catalogXML to set
	 */
	public void setCatalogXML(String catalogXML) {
		this.catalogXML = catalogXML;
	}

	/**
	 * @return the useQueryOntology
	 */
	public boolean isUseQueryOntology() {
		return useQueryOntology;
	}

	/**
	 * @param useQueryOntology the useQueryOntology to set
	 */
	public void setUseQueryOntology(boolean useQueryOntology) {
		this.useQueryOntology = useQueryOntology;
	}

	/**
	 * @return the queryOntology
	 */
	public String getQueryOntology() {
		return queryOntology;
	}

	/**
	 * @param queryOntology the queryOntology to set
	 */
	public void setQueryOntology(String queryOntology) {
		this.queryOntology = queryOntology;
	}

	/**
	 * @return the queryOntologyReference
	 */
	public String getQueryOntologyReference() {
		return queryOntologyReference;
	}

	/**
	 * @param queryOntologyReference the queryOntologyReference to set
	 */
	public void setQueryOntologyReference(String queryOntologyReference) {
		this.queryOntologyReference = queryOntologyReference;
	}

	/**
	 * @return the queryOntologyReferenceIsIRI
	 */
	public boolean isQueryOntologyReferenceIsIRI() {
		return queryOntologyReferenceIsIRI;
	}

	/**
	 * @param queryOntologyReferenceIsIRI the queryOntologyReferenceIsIRI to set
	 */
	public void setQueryOntologyReferenceIsIRI(boolean queryOntologyReferenceIsIRI) {
		this.queryOntologyReferenceIsIRI = queryOntologyReferenceIsIRI;
	}

	/**
	 * @return the removeQueryOntologyReference
	 */
	public boolean isRemoveQueryOntologyReference() {
		return removeQueryOntologyReference;
	}

	/**
	 * @return the writeLabelOWL
	 */
	public boolean isWriteLabelOWL() {
		return writeLabelOWL;
	}

	/**
	 * @param writeLabelOWL the writeLabelOWL to set
	 */
	public void setWriteLabelOWL(boolean writeLabelOWL) {
		this.writeLabelOWL = writeLabelOWL;
	}

	/**
	 * @return the owlOFNFormat
	 */
	public OWLOntologyFormat getOwlOfnFormat() {
		return owlOFNFormat;
	}

	/**
	 * @param removeQueryOntologyReference the removeQueryOntologyReference to set
	 */
	public void setRemoveQueryOntologyReference(boolean removeQueryOntologyReference) {
		this.removeQueryOntologyReference = removeQueryOntologyReference;
	}

	/**
	 * @return the justifyAssertedSubclassesFrom
	 */
	public String getJustifyAssertedSubclassesFrom() {
		return justifyAssertedSubclassesFrom;
	}

	/**
	 * @param justifyAssertedSubclassesFrom the justifyAssertedSubclassesFrom to set
	 */
	public void setJustifyAssertedSubclassesFrom(String justifyAssertedSubclassesFrom) {
		this.justifyAssertedSubclassesFrom = justifyAssertedSubclassesFrom;
	}
	
	/**
	 * @return the ignoreLockFile
	 */
	public boolean isIgnoreLockFile() {
		return ignoreLockFile;
	}

	/**
	 * @param ignoreLockFile the ignoreLockFile to set
	 */
	public void setIgnoreLockFile(boolean ignoreLockFile) {
		this.ignoreLockFile = ignoreLockFile;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OortConfiguration [paths=");
		builder.append(paths);
		builder.append(", base=");
		builder.append(base);
		builder.append(", reasonerName=");
		builder.append(reasonerName);
		builder.append(", enforceEL=");
		builder.append(enforceEL);
		builder.append(", writeELOntology=");
		builder.append(writeELOntology);
		builder.append(", asserted=");
		builder.append(asserted);
		builder.append(", simple=");
		builder.append(simple);
		builder.append(", allowFileOverWrite=");
		builder.append(allowFileOverWrite);
		builder.append(", expandXrefs=");
		builder.append(expandXrefs);
		builder.append(", recreateMireot=");
		builder.append(recreateMireot);
		builder.append(", repairAnnotationCardinality=");
		builder.append(repairAnnotationCardinality);
		builder.append(", expandShortcutRelations=");
		builder.append(expandShortcutRelations);
		builder.append(", allowEquivalentNamedClassPairs=");
		builder.append(allowEquivalentNamedClassPairs);
		builder.append(", macroStrategy=");
		builder.append(macroStrategy);
		builder.append(", checkConsistency=");
		builder.append(checkConsistency);
		builder.append(", writeMetadata=");
		builder.append(writeMetadata);
		builder.append(", writeSubsets=");
		builder.append(writeSubsets);
		builder.append(", justifyAssertedSubclasses=");
		builder.append(justifyAssertedSubclasses);
		builder.append(", sourceOntologyPrefixes=");
		builder.append(sourceOntologyPrefixes);
		builder.append(", executeOntologyChecks=");
		builder.append(executeOntologyChecks);
		builder.append(", forceRelease=");
		builder.append(forceRelease);
		builder.append(", autoDetectBridgingOntology=");
		builder.append(autoDetectBridgingOntology);
		builder.append(", bridgeOntologies=");
		builder.append(bridgeOntologies);
		builder.append(", useReleaseFolder=");
		builder.append(useReleaseFolder);
		builder.append(", gafToOwl=");
		builder.append(gafToOwl);
		builder.append(", skipFormatSet=");
		builder.append(skipFormatSet);
		builder.append(", catalogXML=");
		builder.append(catalogXML);
		builder.append(", useQueryOntology=");
		builder.append(useQueryOntology);
		builder.append(", queryOntology=");
		builder.append(queryOntology);
		builder.append(", queryOntologyReference=");
		builder.append(queryOntologyReference);
		builder.append(", queryOntologyReferenceIsIRI=");
		builder.append(queryOntologyReferenceIsIRI);
		builder.append(", removeQueryOntologyReference=");
		builder.append(removeQueryOntologyReference);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Create a {@link Properties} object for the given configuration.
	 * 
	 * @param config
	 * @return properties
	 */
	public static Properties createProperties(OortConfiguration config) {
		Properties properties = new Properties();
		putValue(properties, "paths", config.paths);
		putValue(properties, "base", config.base);
		putValue(properties, "reasonerName", config.reasonerName);
		putValue(properties, "enforceEL", config.enforceEL);
		putValue(properties, "writeELOntology", config.writeELOntology);
		putValue(properties, "asserted", config.asserted);
		putValue(properties, "simple", config.simple);
		putValue(properties, "allowFileOverWrite", config.allowFileOverWrite);
		putValue(properties, "expandXrefs", config.expandXrefs);
		putValue(properties, "recreateMireot", config.recreateMireot);
		putValue(properties, "repairAnnotationCardinality", config.repairAnnotationCardinality);
		putValue(properties, "expandShortcutRelations", config.expandShortcutRelations);
		putValue(properties, "allowEquivalentNamedClassPairs", config.allowEquivalentNamedClassPairs);
		putValue(properties, "macroStrategy", config.macroStrategy);
		putValue(properties, "checkConsistency", config.checkConsistency);
		putValue(properties, "writeMetadata", config.writeMetadata);
		putValue(properties, "writeSubsets", config.writeSubsets);
		putValue(properties, "justifyAssertedSubclasses", config.justifyAssertedSubclasses);
		putValue(properties, "sourceOntologyPrefixes", config.sourceOntologyPrefixes);
		putValue(properties, "executeOntologyChecks", config.executeOntologyChecks);
		putValue(properties, "forceRelease", config.forceRelease);
		putValue(properties, "autoDetectBridgingOntology", config.autoDetectBridgingOntology);
		putValue(properties, "bridgeOntologies", config.bridgeOntologies);
		putValue(properties, "useReleaseFolder", config.useReleaseFolder);
		putValue(properties, "gafToOwl", config.gafToOwl);
		putValue(properties, "skipFormatSet", config.skipFormatSet);
		putValue(properties, "catalogXML", config.catalogXML);
		putValue(properties, "useQueryOntology", config.useQueryOntology);
		putValue(properties, "queryOntology", config.queryOntology);
		putValue(properties, "queryOntologyReference", config.queryOntologyReference);
		putValue(properties, "queryOntologyReferenceIsIRI", config.queryOntologyReferenceIsIRI);
		putValue(properties, "removeQueryOntologyReference", config.removeQueryOntologyReference);
		return properties;
	}
	
	private static void putValue(Properties properties, String key, MacroStrategy value) {
		if (value != null) {
			properties.put(key, value.name());
		}
	}

	private static void putValue(Properties properties, String key, File value) {
		if (value != null) {
			try {
				properties.put(key, value.getCanonicalPath());
			} catch (IOException e) {
				LOGGER.warn("Could not create canonical path for file: "+value, e);
			}
		}
	}
	
	private static void putValue(Properties properties, String key, boolean value) {
		properties.put(key, Boolean.toString(value));
	}
	
	private static void putValue(Properties properties, String key, String value) {
		if (value != null) {
			properties.put(key, value);
		}
	}
	
	private static void putValue(Properties properties, String key, Collection<String> values) {
		if (values != null && !values.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String string : values) {
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append(escape(string, ','));
			}
			properties.put(key, sb.toString());
		}
	}
	
	/**
	 * Create an {@link OortConfiguration} from the given {@link Properties}.
	 * 
	 * @param properties
	 * @return configuration
	 */
	public static OortConfiguration createOortConfig(Properties properties) {
		OortConfiguration config = new OortConfiguration();
		loadOortConfig(properties, config);
		return config;
	}
	
	/**
	 * Load the given {@link Properties} into the {@link OortConfiguration}.
	 * 
	 * @param properties
	 * @param config
	 */
	public static void loadOortConfig(Properties properties, OortConfiguration config) {
		config.paths = getValue(properties, "paths", config.paths);
		config.base = getValue(properties, "base", config.base);
		config.reasonerName = getValue(properties, "reasonerName", config.reasonerName);
		config.enforceEL = getValue(properties, "enforceEL", config.enforceEL);
		config.writeELOntology = getValue(properties, "writeELOntology", config.writeELOntology);
		config.asserted = getValue(properties, "asserted", config.asserted);
		config.simple = getValue(properties, "simple", config.simple);
		config.allowFileOverWrite = getValue(properties, "allowFileOverWrite", config.allowFileOverWrite);
		config.expandXrefs = getValue(properties, "expandXrefs", config.expandXrefs);
		config.recreateMireot = getValue(properties, "recreateMireot", config.recreateMireot);
		config.repairAnnotationCardinality = getValue(properties, "repairAnnotationCardinality", config.repairAnnotationCardinality);
		config.expandShortcutRelations = getValue(properties, "expandShortcutRelations", config.expandShortcutRelations);
		config.allowEquivalentNamedClassPairs = getValue(properties, "allowEquivalentNamedClassPairs", config.allowEquivalentNamedClassPairs);
		config.macroStrategy = getValue(properties, "macroStrategy", config.macroStrategy);
		config.checkConsistency = getValue(properties, "checkConsistency", config.checkConsistency);
		config.writeMetadata = getValue(properties, "writeMetadata", config.writeMetadata);
		config.writeSubsets = getValue(properties, "writeSubsets", config.writeSubsets);
		config.justifyAssertedSubclasses = getValue(properties, "justifyAssertedSubclasses", config.justifyAssertedSubclasses);
		config.sourceOntologyPrefixes = getValue(properties, "sourceOntologyPrefixes", config.sourceOntologyPrefixes);
		config.executeOntologyChecks = getValue(properties, "executeOntologyChecks", config.executeOntologyChecks);
		config.forceRelease = getValue(properties, "forceRelease", config.forceRelease);
		config.autoDetectBridgingOntology = getValue(properties, "autoDetectBridgingOntology", config.autoDetectBridgingOntology);
		config.bridgeOntologies = getValue(properties, "bridgeOntologies", config.bridgeOntologies);
		config.useReleaseFolder = getValue(properties, "useReleaseFolder", config.useReleaseFolder);
		config.gafToOwl = getValue(properties, "gafToOwl", config.gafToOwl);
		config.skipFormatSet = getValue(properties, "skipFormatSet", config.skipFormatSet);
		config.catalogXML = getValue(properties, "catalogXML", config.catalogXML);
		config.useQueryOntology = getValue(properties, "useQueryOntology", config.useQueryOntology);
		config.queryOntology = getValue(properties, "queryOntology", config.queryOntology);
		config.queryOntologyReference = getValue(properties, "queryOntologyReference", config.queryOntologyReference);
		config.queryOntologyReferenceIsIRI = getValue(properties, "queryOntologyReferenceIsIRI", config.queryOntologyReferenceIsIRI);
		config.removeQueryOntologyReference = getValue(properties, "removeQueryOntologyReference", config.removeQueryOntologyReference);
	}
	
	private static boolean getValue(Properties properties, String key, boolean defaultValue) {
		String property = properties.getProperty(key, null);
		if (property != null) {
			return Boolean.valueOf(property);
		}
		return defaultValue;
	}
	
	private static String getValue(Properties properties, String key, String defaultValue) {
		String property = properties.getProperty(key, null);
		if (property != null) {
			return property.trim();
		}
		return defaultValue;
	}
	
	private static File getValue(Properties properties, String key, File defaultValue) {
		String property = properties.getProperty(key, null);
		if (property != null) {
			return new File(property);
		}
		return defaultValue;
	}
	
	private static MacroStrategy getValue(Properties properties, String key, MacroStrategy defaultValue) {
		String property = properties.getProperty(key, null);
		if (property != null) {
			property = property.trim();
			return MacroStrategy.valueOf(property);
		}
		return defaultValue;
	}
	
	private static Set<String> getValue(Properties properties, String key, Set<String> defaultValue) {
		String property = properties.getProperty(key, null);
		if (property != null) {
			Set<String> result = new HashSet<String>();
			addValues(property, result);
			if (!result.isEmpty()) {
				return result;
			}
		}
		return defaultValue;
	}
	
	private static List<String> getValue(Properties properties, String key, List<String> defaultValue) {
		String property = properties.getProperty(key, null);
		if (property != null) {
			List<String> result = new ArrayList<String>();
			addValues(property, result);
			if (!result.isEmpty()) {
				return result;
			}
		}
		return defaultValue;
	}
	
	private static Vector<String> getValue(Properties properties, String key, Vector<String> defaultValue) {
		String property = properties.getProperty(key, null);
		if (property != null) {
			Vector<String> result = new Vector<String>();
			addValues(property, result);
			if (!result.isEmpty()) {
				return result;
			}
		}
		return defaultValue;
	}
	
	/**
	 * Only public for testing purposes.
	 * 
	 * @param s
	 * @param values
	 */
	public static void addValues(String s, Collection<String> values) {
		int pos = 0;
		int prev = 0;
		while ((prev < s.length()) && (pos < s.length()) && (pos = s.indexOf(',', pos)) >= 0) {
			if ((pos > 0 && s.charAt(pos  - 1) == '\\') == false) {
				if (pos - prev > 0) { // no empty strings
					String value = s.substring(prev, pos);
					values.add(unescape(value));
				}
				prev = pos + 1;
			}
			pos  += 1;
		}
		if (prev < s.length()) {
			values.add(unescape(s.substring(prev)));
		}
	}

	/**
	 *  Only public for testing purposes.
	 * 
	 * @param s
	 * @param escapeChar
	 * @return escaped string
	 */
	public static CharSequence escape(String s, char escapeChar) {
		boolean changed = false;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == ',' || c == '\\') {
				sb.append('\\');
				changed = true;
			}
			sb.append(c);
		}
		if (changed) {
			return sb;
		}
		return s;
	}
	
	/**
	 *  Only public for testing purposes.
	 * 
	 * @param s
	 * @return un-escaped string
	 */
	public static String unescape(String s) {
		boolean changed = false;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\') {
				changed = true;
				// append also next char
				i += 1;
				if (i < s.length()) {
					sb.append(s.charAt(i));
				}
			}
			else {
				sb.append(c);
			}
		}
		if (changed) {
			return sb.toString();
		}
		return s;
	}
	
	/**
	 * Read from a configuration property file and load it 
	 * into a new {@link OortConfiguration} object.
	 * 
	 * @param file
	 * @return configuration
	 * @throws IOException
	 */
	public static OortConfiguration readConfig(File file) throws IOException {
		Properties properties = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			properties.load(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		return createOortConfig(properties);
	}
	
	/**
	 * Load the configuration from a property file into the 
	 * given {@link OortConfiguration}.
	 * 
	 * @param file source property file
	 * @param config target configuration
	 * @throws IOException
	 */
	public static void loadConfig(File file, OortConfiguration config) throws IOException {
		Properties properties = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			properties.load(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		loadOortConfig(properties, config);
	}
	
	/**
	 * Write the given {@link OortConfiguration} into the file as properties.
	 * 
	 * @param file target file
	 * @param config
	 * @throws IOException
	 */
	public static void writeConfig(File file, OortConfiguration config) throws IOException {
		Properties properties = createProperties(config);
		Writer writer = null;
		try {
			writer = new FileWriter(file);
			properties.store(writer , null);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}
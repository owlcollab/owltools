package owltools.gaf.godb;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * Generates a relational database dump.
 * 
 * Currently there is one subclass, for the GO MySQL "lead" database;
 * in principle this is easily extended, e.g. for Chado
 * 
 * Not intended for incremental updates; bulk loading only
 * 
 * @author cjm
 *
 */
public abstract class DatabaseDumper extends Dumper {

	private static Logger LOG = Logger.getLogger(DatabaseDumper.class);

	protected Map<String, Map<Object,Integer>> tableObjIdMap = 
			new HashMap<String, Map<Object,Integer>>();
	//protected Map<Object,Integer> objIdMap = new HashMap<Object,Integer>();
	protected Map<String,Integer> objLastIdMap = new HashMap<String,Integer>();
	protected Set<String> incrementallyLoadedTables = new HashSet<String>(); 
	protected boolean isStrict = false;
	int numInvalidAnnotions = 0;

	protected void cleanup() {
		closeAllPrintStreams();
	}

	protected void dumpRow(PrintStream termStream, Object... vals) {
		int n = 0;
		for (Object v : vals) {
			if (n > 0)
				termStream.print("\t");
			termStream.print(v);
			n++;
		}
		termStream.print("\n");

	}

	protected Integer getId(String table, Object obj) throws ReferentialIntegrityException {
		return getId(table, obj, false);
	}
	protected Integer getId(String table, Object obj, boolean isForceExists) throws ReferentialIntegrityException {
		obj = normalizeObject(obj);
		if (!tableObjIdMap.containsKey(table))
			tableObjIdMap.put(table, new HashMap<Object,Integer>());
		Map<Object, Integer> objIdMap = tableObjIdMap.get(table);
		if (objIdMap.containsKey(obj)) {
			return objIdMap.get(obj);
		}
		//LOG.info("Making a new ID for "+obj+" in "+table);
		if (isForceExists) {			
			throw new ReferentialIntegrityException(table, obj);
		}
		if (!objLastIdMap.containsKey(table)) {
			objLastIdMap.put(table, 0);
		}
		int id = objLastIdMap.get(table) + 1;
		objLastIdMap.put(table, id);
		objIdMap.put(obj, id);
		return id;

	}

	private Object normalizeObject(Object obj) {
		if (obj instanceof OWLObject) {
			return graph.getIdentifier((OWLObject)obj);
		}
		return obj;
	}

	Map<String, PrintStream> printStreamMap = new HashMap<String, PrintStream>();
	protected PrintStream getPrintStream(String t) throws IOException {
		return getPrintStream(t, false);
	}
	protected PrintStream getPrintStream(String t, boolean isAppend) throws IOException {
		if (!printStreamMap.containsKey(t)) {
			LOG.info("Opening table for output: "+t);
			FileOutputStream fos;
			FileUtils.forceMkdir(new File(targetDirectory));
			String path = targetDirectory + "/" + t.toString() + ".txt";
			if (isAppend || this.incrementallyLoadedTables.contains(t)) {
				// TODO - allow option to introspect file for lastId
				fos = new FileOutputStream(path, true);

			}
			else {
				fos = new FileOutputStream(path);
			}
			printStreamMap.put(t, new PrintStream(new BufferedOutputStream(fos)));
		}
		return printStreamMap.get(t);
	}
	protected void closeAllPrintStreams() {
		for (PrintStream s : printStreamMap.values()) {
			LOG.info("Closing stream: "+s);
			s.close();
		}
	}
	protected void closePrintStream(String t) {
		printStreamMap.get(t).close();
		printStreamMap.remove(t);
	}

	protected void showStats() {
		LOG.info("#invalid anns = " + this.numInvalidAnnotions);
	}

}

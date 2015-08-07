package owltools.panther;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Methods to simplify the work with sets of PANTHER trees.
 * See: {@link owltools.panther.PANTHERTree}.
 */
public class PANTHERForest {
	
	private static final Logger LOG = Logger.getLogger(PANTHERForest.class);

	//private final String treeName;
	private final Map<String,Set<PANTHERTree>> bioentityIdentifierToTreeMap;
	private final Map<String,PANTHERTree> treeIdentifierToTree;
	//private final Map<String,String> identifierToLabelMap;
	public int fileCount = 0;
	
	/**
	 * Create an instance for the given primitive array of files.
	 * 
	 * @param pantherDir
	 * @throws IOException 
	 */
	public PANTHERForest (File pantherDir) throws IOException {
	//public PANTHERForest (File treeClassifications, List<File> pFilesCollection) throws IOException {

		String[] exts = new String[1];
		exts[0] = "arbre";
		Collection<File> arbreFiles = FileUtils.listFiles(pantherDir, exts, true);
		List<File> pTreeFiles = new ArrayList<File>(arbreFiles);
		
//		identifierToLabelMap = new HashMap<String,String>();
		bioentityIdentifierToTreeMap = new HashMap<String,Set<PANTHERTree>>();
		treeIdentifierToTree = new HashMap<String,PANTHERTree>();
//
//		// First, loop through the HMM file and capture all of the label
//		// and id mapping that we have there.
//		String tcFileAsStr = FileUtils.readFileToString(treeClassifications);
//		String[] lines = StringUtils.split(tcFileAsStr, "\n");
//		for( String line : lines ){
//			
//			// We just want the first two columns right now, the rest can be considered garbage.
//			String[] columns = StringUtils.split(line, "\t");
//			if( columns != null && columns.length >= 2 ){
//				String tcID = columns[0];
//				String tcLabel = columns[1];
//				// Make sure that they're legit values.
//				if( tcID != null && tcLabel != null && ! tcID.equals("") && ! tcLabel.equals("") ){
//
//					// ...and "*FAMILY NOT NAMED*" is not considered informative in this world.
//					if( ! StringUtils.contains(tcLabel, "FAMILY NOT NAMED")){
//						// And, because all caps is crazy ugly...
//						identifierToLabelMap.put(tcID, StringUtils.lowerCase(tcLabel));
//					}
//				}
//			}
//		}
		
		// Loop through the incoming files to create a map from
		// the annotation data back to a set of PANTHER tools
		// associated with it.
		for( File pFile : pTreeFiles ){
			
			LOG.info("Processing PANTHER tree: " + pFile.getAbsolutePath());
			
			PANTHERTree ptree = new PANTHERTree(pFile);
			
			// Set the tree into the set for later management.
			// Clobber anything in our way.
			String pID = ptree.getTreeID();
			treeIdentifierToTree.put(pID, ptree);
			
			Set<String> aSet = ptree.associatedIdentifierSet();
			fileCount++;
			
			// Associate the identifier with one or more PANTHERTreeFiles.
			for( String id : aSet ){
				
				// Either it's already int here or we have to 
				// add it ourselves.
				if( bioentityIdentifierToTreeMap.containsKey(id) ){
					Set<PANTHERTree> pSet = bioentityIdentifierToTreeMap.get(id);
					pSet.add(ptree);
					bioentityIdentifierToTreeMap.put(id, pSet);
				}else{
					Set<PANTHERTree> pSet = new HashSet<PANTHERTree>();
					pSet.add(ptree);					
					bioentityIdentifierToTreeMap.put(id, pSet);
				}
			}
		}
	}
	
	/**
	 * Return the number of files read into the set.
	 * 
	 * @return count
	 */
	public int getNumberOfFilesInSet(){
		return fileCount;
	}

	/**
	 * Return the number of unique identifiers found in the set.
	 * 
	 * @return count 
	 */
	public int getNumberOfIdentifiersInSet(){
		return bioentityIdentifierToTreeMap.size();
	}

	/**
	 * Return the number of unique bioentity identifiers found in the set.
	 * If nothing was found, return null.
	 * 
	 * @param identifier
	 * @return set of trees
	 */
	public Set<PANTHERTree> getAssociatedTrees(String identifier){
		Set<PANTHERTree> retSet = null;
		
		Set<PANTHERTree> pSet = bioentityIdentifierToTreeMap.get(identifier);
		if( pSet != null && ! pSet.isEmpty()){
			retSet = pSet;
		}
		
		return retSet;
	}

	/**
	 * Return the unique tree identifiers found in the forest.
	 * If nothing was found, return null.
	 * 
	 * @return set 
	 */
	public Set<String> getTreeIDSet(){
		Set<String> pSet = treeIdentifierToTree.keySet();
		return pSet;
	}

	/**
	 * Return the tree for the tree unique identifier,
	 * If nothing was found, return null.
	 * 
	 * @param tree_id the id of the tree we want
	 * @return tree
	 */
	public PANTHERTree getTreeByID(String tree_id){
		return treeIdentifierToTree.get(tree_id);
	}
}

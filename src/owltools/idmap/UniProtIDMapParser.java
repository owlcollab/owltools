package owltools.idmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UniProtIDMapParser extends AbstractMappingParser {
	public Map<String,Map<String,Set<String>>> idMap = new HashMap<String,Map<String,Set<String>>>();
	public Set<String> idTypes = null;
	
	@Override
	public void parse(Reader r) throws IOException {
		reader = new BufferedReader(r);
		
		
		while (true) {
			currentRow  = reader.readLine();
			if(currentRow == null){
				break;
			}

			lineNumber++;

			if (this.currentRow.trim().length() == 0) {
				continue;
			}
			String[] vals = this.currentRow.split("\\t", -1);
			String type = vals[1];
			if (idTypes == null || idTypes.contains(type)) {
				if (!idMap.containsKey(type)) {
					idMap.put(type, new HashMap<String,Set<String>>());
				}
				Map<String, Set<String>> m2 = idMap.get(type);
				String id1 = vals[0];
				if (!m2.containsKey(id1)) {
					m2.put(id1, new HashSet<String>());
				}
				m2.get(id1).add(vals[2]);
			}
		}

	}

}

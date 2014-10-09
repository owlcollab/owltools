package owltools.gaf.lego.server.external;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableLookupService implements ExternalLookupService {
	
	private final Map<String, List<LookupEntry>> entries;
	
	public TableLookupService(Iterable<LookupEntry> dataProvider) {
		entries = new HashMap<String, List<LookupEntry>>();
		for (LookupEntry entry : dataProvider) {
			List<LookupEntry> list = entries.get(entry.id);
			if (list == null) {
				list = new ArrayList<LookupEntry>();
				entries.put(entry.id, list);
			}
			list.add(entry);
		}
	}
	
	@Override
	public List<LookupEntry> lookup(String id) {
		List<LookupEntry> list = entries.get(id);
		if (list == null) {
			list = Collections.emptyList();
		}
		return list;
	}

	@Override
	public LookupEntry lookup(String id, String taxon) {
		LookupEntry entry = null;
		List<LookupEntry> list = entries.get(id);
		if (list != null) {
			for (LookupEntry current : list) {
				if (taxon.equals(current.taxon)) {
					entry = current;
					break;
				}
			}
		}
		return entry;
	}

}

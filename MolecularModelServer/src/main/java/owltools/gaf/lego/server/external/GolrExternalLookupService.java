package owltools.gaf.lego.server.external;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bbop.golr.java.RetrieveGolrBioentities;
import org.bbop.golr.java.RetrieveGolrBioentities.GolrBioentityDocument;

public class GolrExternalLookupService implements ExternalLookupService {
	
	private final RetrieveGolrBioentities client;
	
	public GolrExternalLookupService(String golrUrl) {
		this(new RetrieveGolrBioentities(golrUrl, 2));
	}
	
	protected GolrExternalLookupService(RetrieveGolrBioentities client) {
		this.client = client;
	}

	@Override
	public List<LookupEntry> lookup(String id) {
		List<LookupEntry> result = new ArrayList<LookupEntry>();
		try {
			List<GolrBioentityDocument> bioentites = client.getGolrBioentites(id);
			if (bioentites != null && !bioentites.isEmpty()) {
				result = new ArrayList<ExternalLookupService.LookupEntry>(bioentites.size());
				for(GolrBioentityDocument doc : bioentites) {
					result.add(new LookupEntry(doc.bioentity, doc.bioentity_label, doc.type, doc.taxon));
				}
			}
		}
		catch(IOException exception) {
			return null;
		}
		return result;
	}

	@Override
	public LookupEntry lookup(String id, String taxon) {
		throw new RuntimeException("This method is not implemented.");
	}

}

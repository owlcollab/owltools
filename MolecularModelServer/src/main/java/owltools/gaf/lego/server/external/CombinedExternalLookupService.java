package owltools.gaf.lego.server.external;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombinedExternalLookupService implements ExternalLookupService {
	
	private final Iterable<ExternalLookupService> services;

	/**
	 * @param services
	 */
	public CombinedExternalLookupService(ExternalLookupService...services) {
		this(Arrays.asList(services));
	}
	
	/**
	 * @param services
	 */
	public CombinedExternalLookupService(Iterable<ExternalLookupService> services) {
		this.services = services;
	}

	@Override
	public List<LookupEntry> lookup(String id) {
		List<LookupEntry> result = new ArrayList<LookupEntry>();
		for (ExternalLookupService service : services) {
			List<LookupEntry> cResult = service.lookup(id);
			if (cResult != null && !cResult.isEmpty()) {
				result.addAll(cResult);
			}
		}
		return result;
	}

	@Override
	public LookupEntry lookup(String id, String taxon) {
		LookupEntry result = null;
		for (ExternalLookupService service : services) {
			result = service.lookup(id, taxon);
			if (result != null) {
				break;
			}
		}
		return result;
	}

}

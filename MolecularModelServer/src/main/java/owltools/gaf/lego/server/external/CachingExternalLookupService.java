package owltools.gaf.lego.server.external;

import java.util.Arrays;
import java.util.List;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class CachingExternalLookupService implements ExternalLookupService {
	
	private final LoadingCache<String, List<LookupEntry>> cache;
	private final ExternalLookupService service;
	
	public CachingExternalLookupService(ExternalLookupService service, int size) {
		this.service = service;
		cache = CacheBuilder.newBuilder()
				.maximumSize(size)
				.build(new CacheLoader<String, List<LookupEntry>>() {

					@Override
					public List<LookupEntry> load(String key) {
						return CachingExternalLookupService.this.service.lookup(key);
					}
				});
	}
	
	public CachingExternalLookupService(Iterable<ExternalLookupService> services, int size) {
		this(new CombinedExternalLookupService(services), size);
	}

	public CachingExternalLookupService(int size, ExternalLookupService...services) {
		this(Arrays.asList(services), size);
	}
	
	@Override
	public List<LookupEntry> lookup(String id) {
		return cache.getUnchecked(id);
	}

	@Override
	public LookupEntry lookup(String id, String taxon) {
		LookupEntry entry = null;
		List<LookupEntry> list = cache.getUnchecked(id);
		for (LookupEntry current : list) {
			if (taxon.equals(current.taxon)) {
				entry = current;
				break;
			}
		}
		return entry;
	}

}

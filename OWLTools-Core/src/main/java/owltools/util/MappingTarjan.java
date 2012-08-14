package owltools.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension of the {@link AbstractTarjan} implementation to use internal map
 * for handling the required information for a node. This avoids the creation of
 * node wrappers at the cost of extra overhead of maps.
 * 
 * @param <T>
 */
public class MappingTarjan<T> extends AbstractTarjan<T> {
	
	private final Map<T, Integer> indicies = new HashMap<T, Integer>();
	private final Map<T, Integer> lowlinks = new HashMap<T, Integer>();
	
	@Override
	protected void setIndex(T n, int index) {
		indicies.put(n, Integer.valueOf(index));
	}
	
	@Override
	protected int getIndex(T n) {
		Integer index = indicies.get(n);
		if (index == null) {
			return -1;
		}
		return index.intValue();
	}
	
	@Override
	protected void setLowlink(T n, int lowlink) {
		lowlinks.put(n, Integer.valueOf(lowlink));
	}
	
	@Override
	protected int getLowlink(T n) {
		Integer lowlink = lowlinks.get(n);
		if (lowlink == null) {
			return -1;
		}
		return lowlink.intValue();
	}

	@Override
	protected boolean notEquals(T n1, T n2) {
		return n1.equals(n2) == false;
	}
}
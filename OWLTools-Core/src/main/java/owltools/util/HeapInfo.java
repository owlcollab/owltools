package owltools.util;

import org.apache.log4j.Logger;

public class HeapInfo {
	private static final Logger LOG = Logger.getLogger(HeapInfo.class);

	public static void showHeapStatus() {
		long heapSize = Runtime.getRuntime().totalMemory(); 
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		long heapFreeSize = Runtime.getRuntime().freeMemory(); 

		LOG.info("heap size: " +  formatSize(heapSize));
		LOG.info("heap max size: "+ formatSize(heapMaxSize));
		LOG.info("heap free size: "+ formatSize(heapFreeSize));	
	}

	private static String formatSize(long v) {
		if (v < 1024) return v + " B";
		int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
		return String.format("%.1f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
	}
}
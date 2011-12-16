package owltools.idmap;

import java.util.Map;

import owltools.idmap.IDMappingPIRParser.Types;

public  class IDMapPairWriter extends IDMapHandler {
	public Types t1 = Types.UniProtKB_ID;
	public Types t2 = Types.EntrezGene;

	private int ix1;
	private int ix2;

	public void init() {
		ix1 = typeMap.get(t1);
		ix2 = typeMap.get(t2);
	}

	public boolean process(String[] colVals) {
		if (colVals.length <= Math.max(ix1,ix2))
			return false;
		String[] vals1 = split(colVals[ix1]);
		String[] vals2 = split(colVals[ix2]);
		for (String v1 : vals1) {
			for (String v2 : vals2) {
				System.out.println(v1+"\t"+v2);
			}				
		}
		return true;
	}
	
	public void setPair(String s1, String s2) {
		t1 = Types.valueOf(s1);
		t2 = Types.valueOf(s2);
	}


}

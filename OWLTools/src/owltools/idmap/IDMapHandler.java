package owltools.idmap;

import java.util.Map;

import owltools.idmap.IDMappingPIRParser.Types;

public abstract class IDMapHandler {
	public Map<Types,Integer> typeMap;
	public abstract boolean process(String[] colVals);
	String[] emptyArr = {};
	
	public void init () {
		
	}
	public String[] split(String s) {
		if (s.equals(""))
			return emptyArr;
		return s.split("; ", -1);
	}

}

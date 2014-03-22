package owltools.sim2;

public class CutoffException extends Exception {

	private static final long serialVersionUID = -2061400251377976252L;

	private String measure;
	private Double cutoff;
	private Double value;
	private Class valueType = Double.class;
	
	public CutoffException(String measure, Double cutoff, Double value) {
		this.measure = measure;
		this.cutoff = cutoff;
		this.value = value;
	}

	public CutoffException(String measure, Integer cutoff, Integer value) {
		this.measure = measure;
		this.cutoff = cutoff.doubleValue();
		this.value = value.doubleValue();
		this.valueType = Integer.class;
	}

	public String getMessage() {
		if (valueType == Integer.class) {
			return "CutoffException for "+this.measure+": "+this.value.intValue()+"<"+this.cutoff.intValue();
		} else {
			return "CutoffException for "+this.measure+": "+this.value+"<"+this.cutoff;
		}
	}
	
}

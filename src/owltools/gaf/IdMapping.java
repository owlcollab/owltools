package owltools.gaf;

public class IdMapping{

	private String sourceId;
	private String targetId;
	private String relationship;
	private String mappingSource;
	
	public IdMapping(){
	}

	public IdMapping(String sourceId, String targetId, String relationship,
			String mappingSource) {
		this();
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.relationship = relationship;
		this.mappingSource = mappingSource;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public String getRelationship() {
		return relationship;
	}

	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}

	public String getMappingSource() {
		return mappingSource;
	}

	public void setMappingSource(String mappingSource) {
		this.mappingSource = mappingSource;
	}
	
	
	
	
	
	
}

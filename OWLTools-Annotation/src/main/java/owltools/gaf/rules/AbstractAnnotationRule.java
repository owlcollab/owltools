package owltools.gaf.rules;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.Prediction;
import owltools.graph.OWLGraphWrapper;

public abstract class AbstractAnnotationRule implements AnnotationRule {

	private String ruleId;
	private String name;
	private String status;
	private Date date;
	private String description;
	private Date grandFatheringDate = null;
	
	public abstract Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a);
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GafDocument gafDoc) {
		// per default, do nothing
		return Collections.emptySet();
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GafDocument gafDoc, OWLGraphWrapper graph) {
		// per default, do nothing
		return Collections.emptySet();
	}

	@Override
	public List<Prediction> getPredictedAnnotations(GafDocument gafDoc, OWLGraphWrapper graph) {
		// per default, do nothing
		return Collections.emptyList();
	}

	@Override
	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}

	@Override
	public String getRuleId() {
		return this.ruleId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public boolean isAnnotationLevel() {
		return true;
	}

	@Override
	public boolean isDocumentLevel() {
		return false;
	}
	
	@Override
	public boolean isOwlDocumentLevel() {
		return false;
	}

	@Override
	public boolean isInferringAnnotations() {
		return false;
	}

	@Override
	public boolean hasGrandFathering() {
		return getGrandFatheringDate() != null;
	}

	@Override
	public Date getGrandFatheringDate() {
		return grandFatheringDate;
	}

	@Override
	public void setGrandFatheringDate(Date date) {
		grandFatheringDate = date;
	}
	
}


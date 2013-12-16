package owltools.gaf.lego;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;

import com.google.gson.Gson;

/**
 * 
 * TODO - abstract some of this into a generic OWL to JSON-LD converter
 * 
 * @author cjm
 *
 */
public class MolecularModelJsonRenderer {
	
	private static Logger LOG = Logger.getLogger(MolecularModelJsonRenderer.class);
	
	OWLGraphWrapper graph;

	public enum KEY {
		id,
		label,
		type,
		enabledBy,
		occursIn,
		
		onProperty,
		someValuesFrom
	};
	public enum VAL {
		someValueFrom
	};
	
	Gson gson = new Gson();
	
	public MolecularModelJsonRenderer(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
	}

	public Map renderObject(OWLOntology ont) {
		Map model = new HashMap();
		List<Map> iObjs = new ArrayList<Map>();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			iObjs.add(renderObject(ont, i));
		}
		
		return model;
		
	}
	
	public Map renderObject(OWLOntology ont, OWLNamedIndividual i) {
		Map iObj = new HashMap();
		iObj.put(KEY.id, getId(i));
		iObj.put(KEY.label, getLabel(i));
		
		List<Object> typeObjs = new ArrayList<Object>();		
		for (OWLClassExpression x : i.getTypes(ont)) {
			typeObjs.add(renderObject(ont, x));
		}
		Map<OWLObjectPropertyExpression, Set<OWLIndividual>> pvs = i.getObjectPropertyValues(ont);
		for (OWLObjectPropertyExpression p : pvs.keySet()) {
			//typeObjs.add(renderObject(ont, x));
		}
		iObj.put(KEY.type, typeObjs);
		return iObj;
	}
	
	private Object renderObject(OWLOntology ont, OWLClassExpression x) {
		Map xObj = new HashMap();
		if (x.isAnonymous()) {
			if (x instanceof OWLObjectIntersectionOf) {
				List<Object> yObjs = new ArrayList<Object>();		
				for (OWLClassExpression y : ((OWLObjectIntersectionOf)x).getOperands()) {
					yObjs.add(renderObject(ont, y));
				}
				return yObjs;
			}
			else if (x instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)x;
				xObj.put(KEY.type, VAL.someValueFrom);
				xObj.put(KEY.onProperty, renderObject(ont, svf.getProperty()));
				xObj.put(KEY.someValuesFrom, renderObject(ont, svf.getFiller()));				
			}
			else {
				// TODO
			}
		}
		else {
			return getAtom((OWLNamedObject)x);
		}
		return xObj;
	}

	private Object renderObject(OWLOntology ont,
			OWLObjectPropertyExpression px) {
		if (px.isAnonymous()) {
			return null; // TODO
		}
		else {
			return getAtom((OWLNamedObject)px);
		}
	}

	private Object getLabel(OWLNamedObject i) {
		return graph.getLabel(i);
	}
	
	private Object getAtom(OWLNamedObject i) {
		Map xObj = new HashMap();
		
		LOG.info("atom: "+i+" "+getId(i));
		xObj.put(KEY.id, getId(i));
		xObj.put(KEY.label, getLabel(i));
		String type = null;
		if (i instanceof OWLNamedIndividual) {
			type = "NamedIndividual";
		}
		else if (i instanceof OWLClass) {
			type = "Class";
		}
		else if (i instanceof OWLObjectProperty) {
			type = "ObjectProperty";
		}
		xObj.put(KEY.type, type);
		return xObj;
	}


	private String getId(OWLNamedObject i) {
		return graph.getIdentifier(i).replaceAll(":", "_");
	}

	public String renderJson(OWLOntology ont) {
		Map obj = renderObject(ont);
		return gson.toJson(obj);
	}

}

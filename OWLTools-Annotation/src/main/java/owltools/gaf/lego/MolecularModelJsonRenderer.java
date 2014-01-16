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
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;

import com.google.gson.Gson;

/**
 * A Renderer that takes a MolecularModel (an OWL ABox) and generates Map objects
 * that can be translated to JSON using Gson.
 * 
 * TODO - make this inherit from a generic renderer - use OWLAPI visitor?
 * TODO - abstract some of this into a generic OWL to JSON-LD converter
 * 
 * @author cjm
 *
 */
public class MolecularModelJsonRenderer {
	
	private static Logger LOG = Logger.getLogger(MolecularModelJsonRenderer.class);
	
	OWLGraphWrapper graph;

	/**
	 * JSON-LD keywords for elements of different vocabularies:
	 * 
	 *<li>RDF
	 *<li>OWL
	 *<li>GO
	 *<li>RO
	 *
	 * TODO: use a complex enum to generate IRIs for each
	 *
	 */
	public enum KEY {
		id,
		label,
		type,
		enabledBy,
		occursIn,
		
		onProperty,
		someValuesFrom,
		
		intersectionOf,
		unionOf
	}
	
	/**
	 * merge with KEY?
	 *
	 */
	public enum VAL {
		Restriction,
		someValueFrom
	}
	
	Gson gson = new Gson();
	
	/**
	 * @param graph
	 */
	public MolecularModelJsonRenderer(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
	}

	/**
	 * @param ont
	 * @return Map to be passed to Gson
	 */
	public Map<Object, Object> renderObject(OWLOntology ont) {
		Map<Object, Object> model = new HashMap<Object, Object>();
		List<Map<Object, Object>> iObjs = new ArrayList<Map<Object, Object>>();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			iObjs.add(renderObject(ont, i));
		}
		model.put("individuals", iObjs);
		return model;
		
	}
	
	/**
	 * @param ont
	 * @param i
	 * @return Map to be passed to Gson
	 */
	public Map<Object, Object> renderObject(OWLOntology ont, OWLNamedIndividual i) {
		Map<Object, Object> iObj = new HashMap<Object, Object>();
		iObj.put(KEY.id, getId(i));
		iObj.put(KEY.label, getLabel(i));
		
		List<Object> typeObjs = new ArrayList<Object>();		
		for (OWLClassExpression x : i.getTypes(ont)) {
			typeObjs.add(renderObject(ont, x));
		}
		Map<OWLObjectPropertyExpression, Set<OWLIndividual>> pvs = i.getObjectPropertyValues(ont);
		for (OWLObjectPropertyExpression p : pvs.keySet()) {
			List<Object> valObjs = new ArrayList<Object>();
			for (OWLIndividual v : pvs.get(p)) {
				valObjs.add(getAtom((OWLNamedObject) v));
			}
			iObj.put(getId((OWLNamedObject) p), valObjs);
		}
		iObj.put(KEY.type, typeObjs);
		return iObj;
	}
	
	/**
	 * @param ont
	 * @param x
	 * @return  Object to be passed to Gson
	 */
	private Object renderObject(OWLOntology ont, OWLClassExpression x) {
		Map<Object, Object> xObj = new HashMap<Object, Object>();
		if (x.isAnonymous()) {
			if (x instanceof OWLObjectIntersectionOf) {
				List<Object> yObjs = new ArrayList<Object>();		
				for (OWLClassExpression y : ((OWLObjectIntersectionOf)x).getOperands()) {
					yObjs.add(renderObject(ont, y));
				}
				xObj.put(KEY.intersectionOf, yObjs);
			}
			else if (x instanceof OWLObjectUnionOf) {
				List<Object> yObjs = new ArrayList<Object>();		
				for (OWLClassExpression y : ((OWLObjectUnionOf)x).getOperands()) {
					yObjs.add(renderObject(ont, y));
				}
				xObj.put(KEY.unionOf, yObjs);
			}
			else if (x instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)x;
				xObj.put(KEY.type, VAL.Restriction);
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
		Map<Object, Object> xObj = new HashMap<Object, Object>();
		
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


	// TODO - fix for individuals
	private String getId(OWLNamedObject i) {
		return graph.getIdentifier(i).replaceAll(":", "_");
	}

	public String renderJson(OWLOntology ont) {
		Map<Object, Object> obj = renderObject(ont);
		return gson.toJson(obj);
	}

}

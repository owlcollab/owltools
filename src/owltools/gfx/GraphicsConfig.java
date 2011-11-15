package owltools.gfx;

import java.awt.Color;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLProperty;

public class GraphicsConfig {
	
	public Map<OWLProperty,RelationConfig> relationConfigMap = new HashMap<OWLProperty,RelationConfig>();
	
	public class RelationConfig {
		public OWLProperty property;
		public Color color;
		public Integer maxDepthTotal = null;
		public Integer maxDepthChain = null;
		public Shape childArrowShape;
		public Shape parentArrowShape;
	}

}

package org.bbop.graph;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;

import edu.umd.cs.piccolo.PNode;

public class DefaultNamedChildProvider implements NamedChildProvider {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(DefaultNamedChildProvider.class);

	protected static DefaultNamedChildProvider instance;
	
	public static synchronized DefaultNamedChildProvider getInstance() {
		if (instance == null)
			instance = new DefaultNamedChildProvider();
		return instance;
	}
	
	@Override
	public Collection<Object> getChildNames(PNode node) {
		Collection<Object> out = (Collection<Object>) node.getAttribute("_magic_properties_");
		return out;
	}

	@Override
	public PNode getNamedChild(Object name, PNode node) {
		return (PNode) node.getAttribute(name);
	}

	@Override
	public void setNamedChild(Object name, PNode node, PNode value) {
		Collection<Object> props = getChildNames(node);
		if (props == null) {
			props = new LinkedHashSet<Object>();
			node.addAttribute("_magic_properties_", props);
		}
		if (value != null) {
			if (!props.contains(name))
				props.add(name);
		} else {
			props.remove(name);
		}
		PNode oldValue = (PNode) node.getAttribute(name);
		if (oldValue != null)
			node.removeChild(oldValue);		
		node.addAttribute(name, value);
		if (value != null)
			node.addChild(value);
	}

}

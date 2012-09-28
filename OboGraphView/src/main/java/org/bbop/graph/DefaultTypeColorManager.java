package org.bbop.graph;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.graph.OWLGraphWrapper;
import edu.umd.cs.piccolo.PNode;

public class DefaultTypeColorManager implements TypeColorManager, TypeIconManager {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(DefaultTypeColorManager.class);
	
	// colors
	private final Map<OWLObjectProperty, Color> colorMap = new HashMap<OWLObjectProperty, Color>();
	private final Color subClassOfColor;
	private final List<Color> defaultColors = new LinkedList<Color>();

	// icons
	private final Map<OWLObjectProperty, PNode> iconMap = new HashMap<OWLObjectProperty, PNode>();
	private final PNode subClassOfIcon;
	private final PNode partOfIcon;
	
	private final OWLGraphWrapper graph;
	
	public DefaultTypeColorManager(OWLGraphWrapper graph) {
		this.graph = graph;
		subClassOfColor = Color.blue;
		defaultColors.add(Color.ORANGE);
		defaultColors.add(Color.GREEN);
		defaultColors.add(new Color(150, 150, 0));
		
		subClassOfIcon = IconBuilderUtil.createIcon(new Ellipse2D.Double(0,0,30,30), new Color(102, 102, 153), null, null, new Font("Serif", Font.BOLD, 36), "i", Color.white);
		partOfIcon = IconBuilderUtil.createIcon(new Rectangle2D.Double(0,0,30,30), Color.blue, null, null, new Font("Serif", Font.BOLD, 36), "p", Color.white);
	}
	
	@Override
	public PNode getIcon(OWLObjectProperty property) {
		if (property == null) {
			return (PNode) subClassOfIcon.clone();
		}
		PNode icon = iconMap.get(property);
		if (icon != null) {
			icon = (PNode) icon.clone();
		} else {
			String label = graph.getLabel(property);
			if (label != null && ("part of".equals(label) || "part_of".equals(label))) {
				icon = partOfIcon;
			}
			else {
				String id = graph.getLabelOrDisplayId(property);
				icon = IconBuilderUtil.createIcon(new Rectangle2D.Double(0,0,60,30), Color.black, null, null, new Font("Serif", Font.BOLD, 28), id, Color.white);
			}
			iconMap.put(property, icon);
		}
		return icon;
	}
	
	private static final Random random = new Random();
	
	public Color getRandomColor() {
		return new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
	}

	@Override
	public Paint getColor(OWLObjectProperty property) {
		if (property == null) {
			return subClassOfColor;
		}
		Color out = colorMap.get(property);
		if (out == null) {
			synchronized (defaultColors) {
				if (defaultColors.isEmpty() == false)
					out = defaultColors.remove(0);
				else
					out = getRandomColor();	
			}
			colorMap.put(property, out);
		}
		return out;
	}

}

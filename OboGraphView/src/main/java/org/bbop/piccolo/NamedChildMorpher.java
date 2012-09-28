package org.bbop.piccolo;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbop.graph.DefaultNamedChildProvider;
import org.bbop.graph.NamedChildProvider;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;

public class NamedChildMorpher extends MorphableMorpher {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(NamedChildMorpher.class);

	protected NamedChildProvider provider = DefaultNamedChildProvider.getInstance();

	@Override
	public PCompoundActivity morph(final PNode oldNode, final PNode newNode,
			long duration) {
		PCompoundActivity morphActivity = new PCompoundActivity();

		// a map of nodes that need to be added, mapped to their prospective
		// parents
		final Map<PNode, Object[]> addMap = new LinkedHashMap<PNode, Object[]>();

		final Map<PNode, Object> delMap = new LinkedHashMap<PNode, Object>();
		doMorph(morphActivity, oldNode, newNode, addMap, delMap, duration);
		Runnable postRunnable = new Runnable() {
			@Override
			public void run() {
				for (PNode delNode : delMap.keySet()) {
					PNode parent = delNode.getParent();
					Object name = delMap.get(delNode);
					if (parent == null) {
						logger.info("delNode " + delNode
								+ " has null parent");
						continue;
					}
					provider.setNamedChild(name, parent, null);
				}
			}
		};
		Runnable preRunnable = new Runnable() {
			@Override
			public void run() {
				for (PNode child : addMap.keySet()) {
					Object[] pair = addMap.get(child);
					PNode parent = (PNode) pair[0];
					provider.setNamedChild(pair[1], parent, child);
				}
			}
		};
		morphActivity.addImmediateAction(preRunnable);
		morphActivity.addFinishAction(postRunnable);
		return morphActivity;
	}

	public void doMorph(PCompoundActivity morphActivity, PNode oldNode,
			PNode newNode, Map<PNode, Object[]> addMap,
			Map<PNode, Object> removeMap, long duration) {
		morphActivity.addActivity(super.morph(oldNode, newNode, duration));
		if (newNode == null || oldNode == null) {

			// this can only happen if the oldNode or newNode in the original
			// call to morph() was null. If this happens, we're screwed anyway
		} else {
			Collection<Object> magicProperties = new HashSet<Object>();
			if (provider.getChildNames(oldNode) != null)
				magicProperties.addAll(provider.getChildNames(oldNode));
			if (provider.getChildNames(newNode) != null)
				magicProperties.addAll(provider.getChildNames(newNode));
			for (Object s : magicProperties) {
				PNode oldMagicNode = provider.getNamedChild(s, oldNode);
				PNode newMagicNode = provider.getNamedChild(s, newNode);
				/*
				 * if we've created a new node, we don't want to use that new
				 * node both in the animation layer and the layout provider
				 * layer, because then we would lose the original layout
				 * information for the new node (and that information is useful
				 * to lots of screen focusing routines). So if oldMagicNode is
				 * null and newMagicNode isn't, we replace newMagicNode with
				 * a placeholder node in the layout layer
				 */
				if (oldMagicNode == null && newMagicNode != null) {
					PPath placeHolder = new PPath(newMagicNode
							.getFullBoundsReference());
					provider.setNamedChild(s, newNode, placeHolder);
				}
				if (oldMagicNode == null || newMagicNode == null) {
					morphActivity.addActivity(super.morph(oldMagicNode,
							newMagicNode, duration));
					if (oldMagicNode == null) {
						Object[] pair = { oldNode, s };
						addMap.put(newMagicNode, pair);
					}
					if (newMagicNode == null) {
						removeMap.put(oldMagicNode, s);
					}
				} else {
					doMorph(morphActivity, oldMagicNode, newMagicNode, addMap,
							removeMap, duration);
				}
			}
		}
	}

	public NamedChildProvider getProvider() {
		return provider;
	}

	public void setProvider(NamedChildProvider provider) {
		this.provider = provider;
	}
}

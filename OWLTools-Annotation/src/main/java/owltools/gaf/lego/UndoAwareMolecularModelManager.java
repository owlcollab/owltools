package owltools.gaf.lego;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.lego.UndoAwareMolecularModelManager.UndoMetadata;
import owltools.graph.OWLGraphWrapper;

/**
 * Provide undo and redo operations for the {@link MolecularModelManager}.
 */
public class UndoAwareMolecularModelManager extends MolecularModelManager<UndoMetadata> {
	
	private final Map<String, UndoRedo> allChanges = new HashMap<String, UndoRedo>();
	
	private static class UndoRedo {
		final Deque<ChangeEvent> undoBuffer = new LinkedList<ChangeEvent>();
		final Deque<ChangeEvent> redoBuffer = new LinkedList<ChangeEvent>();
		private UndoMetadata token  = null;
		
		void addUndo(List<OWLOntologyChange> changes, UndoMetadata metadata) {
			addUndo(new ChangeEvent(metadata.userId, changes, System.currentTimeMillis()), metadata);
		}
		
		void addUndo(List<OWLOntologyChange> changes, String userId) {
			token = null;
			undoBuffer.push(new ChangeEvent(userId, changes, System.currentTimeMillis()));
		}
		
		void addUndo(ChangeEvent changes, UndoMetadata token) {
			if (this.token == null || this.token.equals(token) == false) {
				// new event or different event
				undoBuffer.push(changes);
				this.token = token;
			}
			else {
				// append to last event
				ChangeEvent current = undoBuffer.peek();
				if (current != null) {
					current.getChanges().addAll(changes.getChanges());
				}
				else {
					undoBuffer.push(changes);
				}
			}
		}
		
		ChangeEvent getUndo() {
			if (undoBuffer.peek() != null) {
				return undoBuffer.pop();
			}
			return null;
		}
		
		void addRedo(List<OWLOntologyChange> changes, String userId) {
			addRedo(new ChangeEvent(userId, changes, System.currentTimeMillis()));
		}
		
		void addRedo(ChangeEvent changes) {
			redoBuffer.push(changes);
			this.token = null;
		}
		
		ChangeEvent getRedo() {
			if (redoBuffer.peek() != null) {
				return redoBuffer.pop();
			}
			return null;
		}
		
		void clearRedo() {
			redoBuffer.clear();
		}
	}
	
	public static class UndoMetadata {
		public final String userId;
		public final long requestToken;
		
		/**
		 * @param userId
		 */
		public UndoMetadata(String userId) {
			this.userId = userId;
			this.requestToken = System.currentTimeMillis();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ (int) (requestToken ^ (requestToken >>> 32));
			result = prime * result
					+ ((userId == null) ? 0 : userId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			UndoMetadata other = (UndoMetadata) obj;
			if (requestToken != other.requestToken) {
				return false;
			}
			if (userId == null) {
				if (other.userId != null) {
					return false;
				}
			} else if (!userId.equals(other.userId)) {
				return false;
			}
			return true;
		}
	}
	
	/**
	 * Details for a change in a model.
	 */
	public static class ChangeEvent {
		final String userId;
		final List<OWLOntologyChange> changes;
		final long time;
		
		/**
		 * @param userId
		 * @param changes
		 * @param time
		 */
		public ChangeEvent(String userId, List<OWLOntologyChange> changes, long time) {
			this.userId = userId;
			this.changes = new ArrayList<OWLOntologyChange>(changes);
			this.time = time;
		}

		public String getUserId() {
			return userId;
		}

		public List<OWLOntologyChange> getChanges() {
			return changes;
		}

		public long getTime() {
			return time;
		}
	}

	public UndoAwareMolecularModelManager(OWLGraphWrapper graph) throws OWLOntologyCreationException {
		super(graph);
	}

	@Override
	protected void addToHistory(String modelId, LegoModelGenerator model, List<OWLOntologyChange> appliedChanges, UndoMetadata metadata) {
		UndoRedo undoRedo;
		synchronized (allChanges) {
			undoRedo = allChanges.get(modelId);
			if (undoRedo == null) {
				undoRedo = new UndoRedo();
				allChanges.put(modelId, undoRedo);
			}
		}
		synchronized (undoRedo) {
			// append to undo
			undoRedo.addUndo(appliedChanges, metadata);
			// clear redo
			undoRedo.clearRedo();
		}
	}
	
	/**
	 * Undo latest change for the given model.
	 * 
	 * @param modelId
	 * @param userId
	 * @return true if the undo was successful
	 * @throws UnknownIdentifierException
	 */
	public boolean undo(String modelId, String userId) throws UnknownIdentifierException {
		LegoModelGenerator model = checkModelId(modelId);
		return undo(modelId, model, userId);
	}

	/**
	 * Undo latest change for the given model.
	 * 
	 * @param modelId
	 * @param model
	 * @param userId
	 * @return true if the undo was successful
	 */
	public boolean undo(String modelId, LegoModelGenerator model, String userId) {
		UndoRedo undoRedo;
		synchronized (allChanges) {
			undoRedo = allChanges.get(modelId);
		}
		if (undoRedo != null) {
			final OWLOntology abox = model.getAboxOntology();
			synchronized (abox) {
				/* 
				 * WARNING multiple locks (undoRedo and abox) always lock ontology first
				 * to avoid deadlocks!
				 */
				synchronized (undoRedo) {
					// pop from undo
					ChangeEvent event = undoRedo.getUndo();
					if (event == null) {
						return false;
					}

					// invert and apply changes
					List<OWLOntologyChange> invertedChanges = ReverseChangeGenerator.invertChanges(event.getChanges());
					applyChanges(invertedChanges, abox.getOWLOntologyManager());

					// push to redo
					undoRedo.addRedo(event.changes, userId);
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Redo latest change for the given model.
	 * 
	 * @param modelId
	 * @param userId
	 * @return true if the redo was successful
	 * @throws UnknownIdentifierException
	 */
	public boolean redo(String modelId, String userId) throws UnknownIdentifierException {
		LegoModelGenerator model = checkModelId(modelId);
		return redo(modelId, model, userId);
	}
	
	/**
	 * Redo latest change for the given model.
	 * 
	 * @param modelId
	 * @param model
	 * @param userId
	 * @return true if the redo was successful
	 */
	public boolean redo(String modelId, LegoModelGenerator model, String userId) {
		UndoRedo undoRedo;
		synchronized (allChanges) {
			undoRedo = allChanges.get(modelId);
		}
		if (undoRedo != null) {
			final OWLOntology abox = model.getAboxOntology();
			synchronized (abox) {
				/* 
				 * WARNING multiple locks (undoRedo and abox) always lock ontology first
				 * to avoid deadlocks!
				 */
				synchronized (undoRedo) {
					// pop() from redo
					ChangeEvent event = undoRedo.getRedo();
					if (event == null) {
						return false;
					}

					// apply changes
					applyChanges(event.getChanges(), abox.getOWLOntologyManager());

					// push() to undo
					undoRedo.addUndo(event.getChanges(), userId);
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Retrieve the current available undo and redo events.
	 * 
	 * @param modelId
	 * @return pair of undo (left) and redo (right) events
	 */
	public Pair<List<ChangeEvent>, List<ChangeEvent>> getUndoRedoEvents(String modelId) {
		UndoRedo undoRedo = null;
		synchronized (allChanges) {
			undoRedo = allChanges.get(modelId);
		}
		if (undoRedo == null) {
			// return empty of no data is available
			return Pair.of(Collections.<ChangeEvent>emptyList(), Collections.<ChangeEvent>emptyList());
		}
		synchronized (undoRedo) {
			// copy the current lists
			List<ChangeEvent> undoList = new ArrayList<ChangeEvent>(undoRedo.undoBuffer);
			List<ChangeEvent> redoList = new ArrayList<ChangeEvent>(undoRedo.redoBuffer);
			return Pair.of(undoList, redoList);
		}
	}
	
	protected void applyChanges(List<OWLOntologyChange> changes, OWLOntologyManager manager) {
		manager.applyChanges(changes);
	}
	
}

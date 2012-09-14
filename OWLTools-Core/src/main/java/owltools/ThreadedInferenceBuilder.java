package owltools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;

/**
 * Partial parallel implementation of {@link InferenceBuilder}. 
 * Uses threads to parallelize the task of finding redundant axioms.
 */
public class ThreadedInferenceBuilder extends InferenceBuilder {

	private final int threads;
	private final ExecutorService executor;

	/**
	 * @param graph
	 * @param factory
	 * @param enforceEL
	 * @param threads
	 */
	public ThreadedInferenceBuilder(OWLGraphWrapper graph, OWLReasonerFactory factory,
			boolean enforceEL, int threads)
	{
		super(graph, factory, enforceEL);
		this.threads = threads;
		this.executor = Executors.newFixedThreadPool(threads);
	}

	/**
	 * @param graph
	 * @param reasonerName
	 * @param enforceEL
	 * @param threads
	 */
	public ThreadedInferenceBuilder(OWLGraphWrapper graph, String reasonerName,
			boolean enforceEL, int threads)
	{
		super(graph, reasonerName, enforceEL);
		this.threads = threads;
		this.executor = Executors.newFixedThreadPool(threads);
	}

	/**
	 * @param graph
	 * @param reasonerName
	 * @param threads
	 */
	public ThreadedInferenceBuilder(OWLGraphWrapper graph, String reasonerName, int threads) {
		super(graph, reasonerName);
		this.threads = threads;
		this.executor = Executors.newFixedThreadPool(threads);
	}

	/**
	 * @param graph
	 * @param threads
	 */
	public ThreadedInferenceBuilder(OWLGraphWrapper graph, int threads) {
		super(graph);
		this.threads = threads;
		this.executor = Executors.newFixedThreadPool(threads);
	}

	@Override
	protected Set<OWLAxiom> getRedundantAxioms(List<OWLAxiom> axiomsToAdd, OWLOntology ontology,
			OWLReasoner reasoner, OWLDataFactory dataFactory)
	{
		List<Future<Set<OWLAxiom>>> futures = new ArrayList<Future<Set<OWLAxiom>>>();
		List<OWLClass> workSet = new ArrayList<OWLClass>();
		final Set<OWLClass> allClasses = ontology.getClassesInSignature();
		int chunkSize = allClasses.size() / threads;
		for (OWLClass cls : allClasses) {
			workSet.add(cls);
			if (workSet.size() == chunkSize) {
				GetRedundantAxiomsTask task = new GetRedundantAxiomsTask(workSet, ontology, reasoner, axiomsToAdd, dataFactory);
				futures.add(executor.submit(task));
				workSet = new ArrayList<OWLClass>();
			}
			
		}
		if (!workSet.isEmpty()) {
			GetRedundantAxiomsTask task = new GetRedundantAxiomsTask(workSet, ontology, reasoner, axiomsToAdd, dataFactory);
			futures.add(executor.submit(task));
		}
		try {
			Set<OWLAxiom> result = null;
			for(Future<Set<OWLAxiom>> future : futures) {
				Set<OWLAxiom> set = future.get();
				if (result == null) {
					result = set;
				}
				else {
					result.addAll(set);
				}
			}
			return result;
		} catch (InterruptedException exception) {
			throw new RuntimeException(exception);
		} catch (ExecutionException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static class GetRedundantAxiomsTask implements Callable<Set<OWLAxiom>> {
		
		private final List<OWLClass> workSet;
		private final OWLOntology ontology;
		private final OWLReasoner reasoner;
		private final List<OWLAxiom> axiomsToAdd;
		private final OWLDataFactory dataFactory;

		GetRedundantAxiomsTask(List<OWLClass> workSet, OWLOntology ontology, OWLReasoner reasoner, List<OWLAxiom> axiomsToAdd, OWLDataFactory dataFactory) {
			super();
			this.workSet = workSet;
			this.ontology = ontology;
			this.reasoner = reasoner;
			this.axiomsToAdd = axiomsToAdd;
			this.dataFactory = dataFactory;
		}

		@Override
		public Set<OWLAxiom> call() throws Exception {
			Set<OWLAxiom> redundantAxioms = new HashSet<OWLAxiom>();
			for (OWLClass cls : workSet) {
				updateRedundant(cls, ontology, axiomsToAdd, redundantAxioms, reasoner, dataFactory);
			}
			return redundantAxioms;
		}
		
	}
	
	@Override
	public synchronized void dispose() {
		disposeThreadPool();
		super.dispose();
	}

	protected void disposeThreadPool() {
		executor.shutdownNow();
	}

}

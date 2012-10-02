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

import org.apache.log4j.Logger;
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
 * 
 * TODO Use thread count also as limit for ELK.
 */
public class ThreadedInferenceBuilder extends InferenceBuilder {
	
	private static final Logger LOG = Logger.getLogger(ThreadedInferenceBuilder.class);

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
		this.executor = createThreadPool(threads);
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
		this.executor = createThreadPool(threads);
	}

	/**
	 * @param graph
	 * @param reasonerName
	 * @param threads
	 */
	public ThreadedInferenceBuilder(OWLGraphWrapper graph, String reasonerName, int threads) {
		super(graph, reasonerName);
		this.threads = threads;
		this.executor = createThreadPool(threads);
	}

	/**
	 * @param graph
	 * @param threads
	 */
	public ThreadedInferenceBuilder(OWLGraphWrapper graph, int threads) {
		super(graph);
		this.threads = threads;
		this.executor = createThreadPool(threads);
	}

	private ExecutorService createThreadPool(int threads) {
		LOG.info("Creating thread pool with "+threads+" threads for inference builder");
		return Executors.newFixedThreadPool(threads);
	}

	@Override
	protected Set<OWLAxiom> getRedundantAxioms(OWLOntology ontology,
			OWLReasoner reasoner, OWLDataFactory dataFactory)
	{
		LOG.info("Start parallel execution.");
		List<Future<Set<OWLAxiom>>> futures = new ArrayList<Future<Set<OWLAxiom>>>();
		List<OWLClass> workSet = new ArrayList<OWLClass>();
		final Set<OWLClass> allClasses = ontology.getClassesInSignature();
		int chunkSize = allClasses.size() / threads;
		int threadCount = 1;
		for (OWLClass cls : allClasses) {
			workSet.add(cls);
			if (workSet.size() == chunkSize) {
				GetRedundantAxiomsTask task = new GetRedundantAxiomsTask(workSet, ontology, reasoner, dataFactory, Integer.toString(threadCount));
				threadCount += 1;
				futures.add(executor.submit(task));
				workSet = new ArrayList<OWLClass>();
			}
			
		}
		if (!workSet.isEmpty()) {
			GetRedundantAxiomsTask task = new GetRedundantAxiomsTask(workSet, ontology, reasoner, dataFactory, Integer.toString(threadCount));
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
			LOG.info("Finished parallel execution.");
			return result;
		} catch (InterruptedException exception) {
			throw new RuntimeException(exception);
		} catch (ExecutionException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static class GetRedundantAxiomsTask implements Callable<Set<OWLAxiom>> {
		
		private final String id;
		private final List<OWLClass> workSet;
		private final OWLOntology ontology;
		private final OWLReasoner reasoner;
		private final OWLDataFactory dataFactory;

		GetRedundantAxiomsTask(List<OWLClass> workSet, OWLOntology ontology, OWLReasoner reasoner, OWLDataFactory dataFactory, String id) {
			super();
			this.id = id;
			this.workSet = workSet;
			this.ontology = ontology;
			this.reasoner = reasoner;
			this.dataFactory = dataFactory;
		}

		@Override
		public Set<OWLAxiom> call() throws Exception {
			int size = workSet.size();
			int steps = (size / 10) + 1;
			LOG.info("Start thread "+id+" with classes count: "+size);
			Set<OWLAxiom> redundantAxioms = new HashSet<OWLAxiom>();
			int count = 0;
			for (OWLClass cls : workSet) {
				updateRedundant(cls, ontology, redundantAxioms, reasoner, dataFactory);
				count += 1;
				if (count % steps == 0) {
					LOG.info("Thread "+id+" Progress: "+count+"/"+size);
				}
			}
			LOG.info("Done thread "+id);
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

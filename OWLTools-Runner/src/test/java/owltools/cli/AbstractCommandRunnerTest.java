package owltools.cli;

import org.junit.After;
import org.junit.Before;

import owltools.OWLToolsTestBasics;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class AbstractCommandRunnerTest extends OWLToolsTestBasics {
	
	protected CommandRunner runner;
	
	/**
	 * This method is annotated with {@link Before}. This means that it will be
	 * executed before each test method call.
	 * 
	 * @see #after()
	 */
	@Before
	public final void before() {
		init();
	}
	
	/**
	 * This method is annotated with {@link After}. This means, that it will be
	 * executed after each test method call.
	 * 
	 * @see #before()
	 */
	@After
	public final void after() {
		destroyCommendRunner(runner);
	}
	
	private final void init() {
		runner = createCommandRunner();
		// Do not kill the VM with System.exit, while running tests
		runner.exitOnException = false;
	}
	
	/**
	 * Create the {@link CommandRunner} instance. Overwrite this method to
	 * create custom instances.
	 * 
	 * @return instance, never null
	 * 
	 * @see #destroyCommendRunner(CommandRunner)
	 */
	protected CommandRunner createCommandRunner() {
		return new CommandRunner();
	}
	
	/**
	 * Cleanup and destroy the current {@link CommandRunner} instance. Overwrite
	 * this method to implement a custom cleanup.
	 * 
	 * @param runner
	 * 
	 * @see #createCommandRunner()
	 */
	protected void destroyCommendRunner(CommandRunner runner) {
		//Do nothing, overwrite this method.
	}
	
	protected void load(String file) throws Exception {
		String path = getResource(file).getAbsolutePath();
		run(path);		
	}
	
	protected void run(String[] args) throws Exception {
		runner.runSingleIteration(args);
	}
	protected void run(String argStr) throws Exception {
		run(argStr.split(" "));
	}
	
	protected void run(String[] args, String[] expectedLines) {
		// TODO
	}
	
	public String path(String in) {
		return getResource(in).getAbsolutePath();
	}

	
}

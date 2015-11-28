package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerInferExistentialsTest extends AbstractCommandRunnerTest {
	

	
	@Test
	public void testInfer() throws Exception {
		load("limb_gci.owl");
		run("--reasoner mexr --assert-inferred-svfs -p http://x.org/part-of");
		run("-o target/limb-inferred.owl");
		
		
	}

	@Test
	public void testInferGCI() throws Exception {
		load("limb_gci.owl");
		run("--reasoner elk --materialize-gcis");
		run("-o target/limb-inferred-by-gci.owl");
		
		
	}


	
	
}

package owltools.cli;

import java.io.File;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerDiffTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testDiff() throws Exception {
        load("go_sample_mf_subset.obo");
        load("go_xp_predictor_test_subset.obo");
		run("--diff -f obo --o1r target/o1r.owl --o2r target/o2r.owl --od target/od.owl");
		
	}
	
	
}

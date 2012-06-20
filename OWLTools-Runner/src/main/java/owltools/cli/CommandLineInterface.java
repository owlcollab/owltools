package owltools.cli;


/**
 * Wrapper class to provide a stable class name for the main method.
 */
public class CommandLineInterface {
	// Do not move or rename this class.

	public static void main(String[] args) throws Exception {
		CommandRunner cr = new JsCommandRunner();
		cr.run(args);
	}
}

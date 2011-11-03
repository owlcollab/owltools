package owltools.cli;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.cli.CommandRunner.OptionException;
import owltools.sim.SimEngine.SimilarityAlgorithmException;

public class CommandLineInterface {

	private static Logger LOG = Logger.getLogger(CommandLineInterface.class);
	
	public static void main(String[] args) throws OWLOntologyCreationException, IOException, FrameMergeException, SimilarityAlgorithmException, OWLOntologyStorageException, OptionException {
		CommandRunner cr = new CommandRunner();
		cr.run(args);
	}
}

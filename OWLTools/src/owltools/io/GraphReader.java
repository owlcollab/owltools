package owltools.io;

import java.io.IOException;

import owltools.graph.OWLGraphWrapper;

public interface GraphReader {

	public void read() throws IOException;
	public void read(String file) throws IOException;
}

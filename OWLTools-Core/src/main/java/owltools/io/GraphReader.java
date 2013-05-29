package owltools.io;

import java.io.IOException;

public interface GraphReader {

	public void read() throws IOException;
	public void read(String file) throws IOException;
}

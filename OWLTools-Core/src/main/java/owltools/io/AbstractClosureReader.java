package owltools.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import owltools.graph.OWLGraphWrapper;

/**
 * 
 * @author cjm
 *
 */
public abstract class AbstractClosureReader implements GraphReader {

	protected InputStream stream;
	OWLGraphWrapper graph;

	public AbstractClosureReader(InputStream stream) {
		super();
		this.stream = stream;
	}

	public AbstractClosureReader(String file) {
		super();
		setStream(file);
	}
	
	public AbstractClosureReader(OWLGraphWrapper g) {
		super();
		graph = g;
	}
	
	public InputStream getStream() {
		return stream;
	}

	public void setStream(InputStream stream) {
		this.stream = stream;
	}
	
	public void setStream(String file) {
		try {
			this.stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void read(String file) throws IOException {
		setStream(file);
		read();
	}
	
	public abstract void read() throws IOException;
}


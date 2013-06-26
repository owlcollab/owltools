package owltools.gaf.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import owltools.gaf.GeneAnnotation;

/**
 * Write a Gene Annotation File to a given stream or file.
 */
public class GafWriter extends AbstractGafWriter {

	protected PrintStream stream;

	public GafWriter() {
		super();
	}
	
	public PrintStream getStream() {
		return stream;
	}

	public void setStream(PrintStream stream) {
		this.stream = stream;
	}
	
	public void setStream(String file) {
		setStream(new File(file));
	}
	
	public void setStream(File file) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
			this.stream = new PrintStream(new BufferedOutputStream(fos));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Could not open file: "+file.getAbsolutePath(), e);
		}
	}
	
	protected void print(String s) {
		stream.print(s);
	}

	@Override
	protected void end() {
		IOUtils.closeQuietly(stream);
	}

	
	/**
	 * Helper class to create a list of all {@link GeneAnnotation} lines,
	 * excluding any headers.<br>
	 * Can be used for sorting.
	 */
	public static class BufferedGafWriter extends AbstractGafWriter {
		
		private final List<String> lines = new ArrayList<String>();
		private StringBuilder current = new StringBuilder();

		@Override
		public void writeHeader(List<String> comments) {
			// do nothing
		}

		@Override
		protected void print(String s) {
			current.append(s);
		}

		@Override
		protected void nl() {
			super.nl();
			lines.add(current.toString());
			current = new StringBuilder();
		}

		@Override
		protected void end() {
			// do nothing
		}
		
		public List<String> getLines() {
			return lines;
		}
	}
	
}

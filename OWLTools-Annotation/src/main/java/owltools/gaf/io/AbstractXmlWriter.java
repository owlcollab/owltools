package owltools.gaf.io;

import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Helper to write properly formated XML using an {@link XMLStreamWriter}.
 */
public abstract class AbstractXmlWriter {
	
	private final XMLOutputFactory factory;
	private final String indentString;

	protected AbstractXmlWriter(String indentString) {
		super();
		this.indentString = indentString;
		factory = XMLOutputFactory.newInstance();
		
	}

	protected XMLStreamWriter createWriter(OutputStream outputStream) throws XMLStreamException {
		XMLStreamWriter writer = factory.createXMLStreamWriter(outputStream);
		PrettyPrintHandler handler = new PrettyPrintHandler(writer, indentString);
		return (XMLStreamWriter) Proxy.newProxyInstance(XMLStreamWriter.class.getClassLoader(),
				new Class[] { XMLStreamWriter.class },
				handler);
	}

	private static class PrettyPrintHandler implements InvocationHandler {

		private XMLStreamWriter target;

		private int depth = 0;
		private Map<Integer, Boolean> hasChild = new HashMap<Integer, Boolean>();

		private final String indentString;
		private static final String LINEFEED_CHAR = "\n";

		public PrettyPrintHandler(XMLStreamWriter target, String indentString) {
			this.target = target;
			this.indentString = indentString;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			String m = method.getName();

			// Needs to be BEFORE the actual event, so that for instance the
			// sequence writeStartElem, writeAttr, writeStartElem, writeEndElem,
			// writeEndElem
			// is correctly handled

			if ("writeStartElement".equals(m)) {
				// update state of parent node
				if (depth > 0) {
					hasChild.put(depth - 1, true);
				}

				// reset state of current node
				hasChild.put(depth, false);

				// indent for current depth
				target.writeCharacters(LINEFEED_CHAR);
				target.writeCharacters(repeat(depth, indentString));

				depth++;
			}

			if ("writeEndElement".equals(m)) {
				depth--;

				if (hasChild.get(depth) == true) {
					target.writeCharacters(LINEFEED_CHAR);
					target.writeCharacters(repeat(depth, indentString));
				}

			}

			if ("writeEmptyElement".equals(m)) {
				// update state of parent node
				if (depth > 0) {
					hasChild.put(depth - 1, true);
				}

				// indent for current depth
				target.writeCharacters(LINEFEED_CHAR);
				target.writeCharacters(repeat(depth, indentString));

			}

			method.invoke(target, args);

			return null;
		}

		private String repeat(int d, String s) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < d; i++) {
				sb.append(s);
			}
			return sb.toString();
		}
	}
}

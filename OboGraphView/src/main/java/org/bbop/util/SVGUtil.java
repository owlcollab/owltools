package org.bbop.util;

import java.io.IOException;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

public class SVGUtil {

	public static GraphicsNode getSVG(String uri) throws IOException {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
		Document doc = f.createDocument(uri);
		GVTBuilder builder = new GVTBuilder();
		GraphicsNode node = builder.build(new BridgeContext(
				new UserAgentAdapter() {
				}), doc);
		return node;
	}

}

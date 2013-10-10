package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.util.MinimalModelGeneratorTest;

public abstract class AbstractLegoModelGeneratorTest extends OWLToolsTestBasics {
	private static Logger LOG = Logger.getLogger(AbstractLegoModelGeneratorTest.class);

	static{
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		//Logger.getLogger("org.semanticweb.elk.reasoner.indexing.hierarchy").setLevel(Level.ERROR);
	}
	LegoModelGenerator ni;
	Writer w;
	

	
	protected void write(String s) throws IOException {
		w.append(s);
	}
	protected void writeln(String s) throws IOException {
		LOG.info(s);
		w.append(s + "\n");
	}

	protected String render(String x) {
		return x + " ! " + ni.getLabel(x);
	}
	protected String render(OWLObject x) {
		if (x == null) {
			return "null";
		}
		return x + " ! " + ni.getLabel(x);
	}


}

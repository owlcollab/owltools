package owltools.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import de.tudresden.inf.lat.jcel.owlapi.main.JcelReasoner;
import owltools.gaf.lego.MolecularModelManager;
import owltools.graph.OWLGraphWrapper;
import owltools.sim2.OwlSim;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;
import uk.ac.manchester.cs.jfact.JFactFactory;

/**
 * 
 * See http://code.google.com/p/owltools/wiki/WebServices
 * 
 * This is the core of the existing OWLTools services implementation
 * 
 * It takes web API calls, creates an OWLHandler object and calls the appropriate method
 * 
 * see OWLHandler for details
 * 
 * @author cjm
 *
 */
public class OWLServer extends AbstractHandler
{

	private static Logger LOG = Logger.getLogger(OWLServer.class);

	OWLGraphWrapper graph;
	Map<String,OWLReasoner> reasonerMap = new HashMap<String,OWLReasoner>();
	OwlSim sos = null;
	MolecularModelManager molecularModelManager = null;

	public OWLServer(OWLGraphWrapper g) {
		super();
		graph = g;
	}
	public OWLServer(OWLGraphWrapper g, OwlSim sos2) {
		super();
		graph = g;
		sos = sos2;
	}

	public void handle(String target,
			Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response) 
					throws IOException, ServletException
					{
		String path = request.getPathInfo();
		baseRequest.setHandled(true);

		OWLHandler handler = new OWLHandler(this, graph, request, response);
		if (sos != null)
			handler.setOwlSim(sos);

		LOG.info("Request "+path);
		path = path.replace("/owlsim/", "/");
		String[] toks = path.split("/");
		String m;
		if (toks.length == 0) {
			m = "top";
		}
		else {
			m = toks[1];
		}
		if (m.contains(".")) {
			String[] mpa = m.split("\\.", 2);
			m = mpa[0];
			handler.setFormat(mpa[1]);
		}
		handler.setCommandName(m);
		Class[] mArgs = new Class[0];
		Method method = null;
		try {
			method = handler.getClass().getMethod(m + "Command", mArgs);
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (method != null) {
			try {
				LOG.info("Method="+method);
				Object[] oArgs = new Object[0];
				method.invoke(handler, oArgs);
				handler.printCachedObjects();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
					
	}

	public synchronized OWLReasoner getReasoner(String reasonerName) {
		// TODO - reasoner synchronization
		if (reasonerMap.containsKey(reasonerName))
			return reasonerMap.get(reasonerName);
		OWLOntology ont = graph.getSourceOntology();
		OWLReasonerFactory reasonerFactory = null;
		OWLReasoner reasoner = null;
		LOG.info("Creating reasoner:"+reasonerName);
		if (reasonerName == null || reasonerName.equals("default")) {
			if (graph.getReasoner() != null)
				return graph.getReasoner();
			reasonerFactory = new ElkReasonerFactory();
		}
		else if (reasonerName == null || reasonerName.equals("factpp"))
			reasonerFactory = new FaCTPlusPlusReasonerFactory();
		else if (reasonerName.equals("hermit")) {
			reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();			
		}
		else if (reasonerName.equals("elk")) {
			reasonerFactory = new ElkReasonerFactory();	
		}
		else if (reasonerName.equals("jfact")) {
			reasonerFactory = new JFactFactory();	
		}
		else if (reasonerName.equals("jcel")) {
			reasoner = new JcelReasoner(ont);
			reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			return reasoner;
		}
		else if (reasonerName.equals("structural")) {
			reasonerFactory = new StructuralReasonerFactory();
		}
		else {
			// TODO
			System.out.println("no such reasoner: "+reasonerName);
		}
		if (reasoner == null)
			reasoner = reasonerFactory.createReasoner(ont);	
		reasonerMap.put(reasonerName, reasoner);
		return reasoner;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jetty.server.handler.AbstractHandler#destroy()
	 */
	@Override
	public void destroy() {
		// clean up reasoners
		Set<String> reasonerNames = reasonerMap.keySet();
		Iterator<String> iterator = reasonerNames.iterator();
		while (iterator.hasNext()) {
			String reasonerName = iterator.next();
			OWLReasoner reasoner = reasonerMap.remove(reasonerName);
			if (reasoner != null) {
				reasoner.dispose();
			}
		}
		super.destroy();
	}
}
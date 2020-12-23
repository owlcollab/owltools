package owltools.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.URIUtil;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import owltools.graph.OWLGraphWrapper;
import owltools.sim2.OwlSim;

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

		// TODO/BUG: A real server interface that we can operate on.
		// Check the incoming args (if any) to see if we're going to use JSONP wrapping.
		String jsonp_callback = request.getParameter("json.wrf");
		// Check to make sure that the jsonp callback argument is legit.
		if( jsonp_callback == null || jsonp_callback.isEmpty() ){
			jsonp_callback = null; // a miss is as good as a mile
		}
		// About to sin...
		// TODO: We'd like to change the header to javascript here--no longer JSON.
		// TODO: We'll happily wrap non-JSON things here as well.
		// The closer is later on.
		if( jsonp_callback != null ){ response.getWriter().write(jsonp_callback + '('); }
		
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
		if ("status".equals(m)) {
			// report status
			reportStatus(baseRequest, request, response);
		}
		else {
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
		// TODO/BUG: Remove for a real system
		// Close in case of JSONP. See above.
		if( jsonp_callback != null ){ response.getWriter().write(')'); }			
	}

	private void reportStatus(Request baseRequest,
			HttpServletRequest request,
			HttpServletResponse response) {
		Map<String, Object> jsonObj = new HashMap<String, Object>();
		// name
		jsonObj.put("name", "owlserver");

		// okay
		jsonObj.put("okay", Boolean.TRUE);

		// date in GMT
		Date localTime = new Date(); 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		jsonObj.put("date", sdf.format(localTime));
		
		// location
		// use the request URL, the server does not know its address
		jsonObj.put("location", getLocationInfo(request));

		// offerings (optional list of values)
		// for now empty

		try {
			Gson gson = new GsonBuilder().create();
			String js = gson.toJson(jsonObj);
			response.getWriter().write(js);
		} catch (IOException e) {
			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			try {
				response.sendError(code , "Could not send status message due to an internal issue: "+e.getMessage());
			} catch (IOException e1) {
				LOG.error("Could not send error message for original IOException: "+e.getMessage(), e1);
			}
		}
	}
	
	private CharSequence getLocationInfo(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		String scheme = request.getScheme();
        int port = request.getServerPort();

        sb.append(scheme);
        sb.append("://");
        sb.append(request.getServerName());
        if (port>0 && 
            ((scheme.equalsIgnoreCase(URIUtil.HTTP) && port != 80) || 
             (scheme.equalsIgnoreCase(URIUtil.HTTPS) && port != 443)))
        {
        	sb.append(':');
        	sb.append(port);
        }
		return sb;
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
			reasonerFactory = new ReasonerFactory();
		}
		else if (reasonerName.equals("hermit")) {
			reasonerFactory = new ReasonerFactory();	
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
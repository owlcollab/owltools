package owltools.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import owltools.cli.tools.CLIMethod;
import owltools.graph.OWLGraphWrapper;

/**
 * Provide a JavaScript interpreter for OWLTools. 
 * Allows to use/modify the internal {@link OWLGraphWrapper} with dynamic scripts.
 */
public class JsCommandRunner extends Sim2CommandRunner {

	private static final Logger LOG = Logger.getLogger(JsCommandRunner.class);
	
	private final ScriptEngineManager jsManager;

	public JsCommandRunner() {
		super();
		jsManager = new ScriptEngineManager();
	}

	private ScriptEngine getEngine() {
		return jsManager.getEngineByName("js");
	}
	
	/**
	 * Run the given js with the current graph (and reasoner).
	 * 
	 * @param opts
	 */
	@CLIMethod({"-js","--javascript"})
	public void runJavaScript(Opts opts) {
		List<String> scriptFiles = new ArrayList<String>();
		String inlineScript = null;
		String methodName = null; // optional
		
		while (opts.hasArgs()) {
			if (opts.nextEq("-m") || opts.nextEq("--method")) {
				methodName = opts.nextOpt();
			}
			else if (opts.nextEq("-i") || opts.nextEq("--inline")) {
				inlineScript = opts.nextOpt();
			}
			else if (opts.nextEq("//")) {
				break;
			}
			else {
				scriptFiles.add(opts.nextOpt());
			}
		}
		
		if (scriptFiles.isEmpty() && inlineScript == null) {
			LOG.error("No script files found and no inline script was specified.");
			exit(-1);
		}
		
		String script = loadScripts(inlineScript, scriptFiles);
		ScriptEngine engine = getEngine();
		
		try {
			// put the graph and reasoner in the JS context
			engine.put("graph", this.g);
			engine.put("reasoner", this.reasoner);
			
			run(engine, script, methodName);
		} catch (ScriptException e) {
			printScript(script);
			LOG.error("Error during script execution", e);
		} catch (NoSuchMethodException e) {
			printScript(script);
			LOG.error("Error during script execution", e);
		}
	}
	
	/**
	 * Load and combine all scripts into one single script to be run.
	 * 
	 * @param inline
	 * @param scriptFiles
	 * @return script
	 */
	private String loadScripts(String inline, Iterable<String> scriptFiles) {

		StringBuilder sb = new StringBuilder();
		if (inline != null) {
			sb.append(inline);
		}
		for (String scriptFile : scriptFiles) {
			File file = new File(scriptFile);
			try {
				String script = FileUtils.readFileToString(file);
				if (sb.length() > 0) {
					sb.append("\n\n");
				}
				sb.append(script);
			} catch (IOException e) {
				LOG.error("Could not load script from file: "+scriptFile, e);
				exit(-1);
			}
		}
		return sb.toString();
	}
	
	private void run(ScriptEngine engine, String script, String methodName)
			throws ScriptException, NoSuchMethodException
	{
		// either create the functions to be called or runs the script
		engine.eval(script);
		if (methodName != null) {
			// optional
			Invocable invocableEngine = (Invocable) engine;
			// call a parameterless function
			invocableEngine.invokeFunction(methodName);
		}
	}
	
	/**
	 * Print the script with additional line numbers for easy debugging.
	 * 
	 * @param script
	 */
	private static void printScript(String script) {
		PrintWriter writer = new PrintWriter(System.out);
		writer.println();
		writer.println("Script:");
		int pos;
		int prev = 0;
		int count = 1;
		while ((pos = script.indexOf('\n', prev)) >= 0) {
			writer.print(count);
			if (count < 10) {
				writer.print(' ');
			}
			if (count < 100) {
				writer.print(' ');
			}
			writer.print(' ');
			writer.println(script.substring(prev, pos));

			prev = pos + 1;
			count += 1;
		}
		if (prev < script.length()) {
			writer.print(count);
			if (count < 10) {
				writer.print(' ');
			}
			if (count < 100) {
				writer.print(' ');
			}
			writer.print(' ');
			writer.println(script.substring(prev));
		}
		writer.println();
		writer.println();
		writer.flush();
	}
}

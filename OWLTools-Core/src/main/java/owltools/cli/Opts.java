package owltools.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Helper for easy handling of command line parameters and input.
 */
public class Opts {
	
	private int i = 0;
	private String[] args;
	private boolean helpMode = false;

	/**
	 * Create a new instance for the given command-line parameters.
	 * 
	 * @param args command-line parameter array
	 */
	public Opts(String[] args) {
		super();
		this.i = 0;
		this.args = args;
	}
	
	/**
	 * Create a new instance for the given command-line parameters.
	 * 
	 * @param args list of command-line parameters
	 */
	public Opts(List<String> args) {
		this(args.toArray(new String[args.size()]));
	}

	/**
	 * @param helpMode the helpMode to set
	 */
	public void setHelpMode(boolean helpMode) {
		this.helpMode = helpMode;
	}

	/**
	 * @return the helpMode
	 */
	public boolean isHelpMode() {
		return helpMode;
	}

	/**
	 * @return true, if there are further parameters to handle. 
	 */
	public boolean hasArgs() {
		return i < args.length;
	}
	
	/**
	 * @return if there is a next parameter, which is an option flag
	 */
	public boolean hasOpts() {
		return hasArgs() && args[i].startsWith("-");
	}
	
	/**
	 * Check if the option flag is in the remaining parameters
	 * 
	 * @param opt
	 * @return true, if one of the remaining parameters equals the given string.
	 */
	public boolean hasOpt(String opt) {
		for (int j=i; j<args.length; j++) {
			if (args[j].equals(opt))
				return true;
		}
		return false;
	}

	public boolean nextEq(String eq) {
		if (helpMode) {
			System.out.println("    "+eq);
			return false;
		}
		if (eq.contains("|")) {
			return nextEq(eq.split("\\|"));
		}
		if (hasArgs()) {
			if (args[i].equals(eq)) {
				i++;
				return true;
			}
		}
		return false;
	}

	boolean nextEq(String[] eqs) {
		for (String eq : eqs) {
			if (nextEq(eq))
				return true;
		}
		return false;
	}
	public boolean nextEq(Collection<String> eqs) {
		for (String eq : eqs) {
			if (nextEq(eq))
				return true;
		}
		return false;
	}
	public List<String> nextList() {
		ArrayList<String> sl = new ArrayList<String>();
		while (hasArgs()) {
			if (args[i].equals("//")) {
				i++;
				break;
			}
			if (args[i].startsWith("-"))
				break;
			sl.add(args[i]);
			i++;
		}
		return sl;
	}
	public String nextOpt() {
		String opt = args[i];
		i++;
		return opt;
	}
	public String peekArg() {
		if (hasArgs())
			return args[i];
		return null;
	}
	public boolean nextArgIsHelp() {
		if (hasArgs() && (args[i].equals("-h")
				|| args[i].equals("--help"))) {
			nextOpt();
			return true;
		}
		return false;
	}

	/**
	 * Send a fail. WARNING: This will terminate the VM.
	 * Do NOT use in a framework.
	 * 
	 * Uses System.err to print the error message. 
	 */
	public void fail() {
		System.err.println("cannot process: "+args[i]);
		System.exit(1);

	}

	/**
	 * Write an info. WARNING: This will terminate the VM.
	 * Do NOT use in a framework.
	 * 
	 * Uses System.out to print the message. 
	 * 
	 * @param params
	 * @param desc
	 */
	public void info(String params, String desc) {
		if (this.nextArgIsHelp()) {
			System.out.println(args[i-2]+" "+params+"\t   "+desc);
			System.exit(0);
		}
	}
}
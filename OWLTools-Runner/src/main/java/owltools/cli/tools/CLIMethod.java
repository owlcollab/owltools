package owltools.cli.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import owltools.cli.Opts;

/**
 * Annotation indicating, that this method can be called from the command line
 * interface.
 * <br/>
 * Annotated methods must be public and have exactly one parameter of type
 * {@link Opts}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CLIMethod {

	/**
	 * @return the parameters to be matched from the command-line
	 */
	String[] value();
}

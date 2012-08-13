package owltools.ontologyverification.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class annotation indicating the time-point for executing checks after the
 * loading of the ontology (inclduing supports and imports) in OORT
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface AfterLoading {

}

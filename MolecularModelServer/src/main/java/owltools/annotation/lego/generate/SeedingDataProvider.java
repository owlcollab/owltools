package owltools.annotation.lego.generate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import owltools.gaf.Bioentity;
import owltools.gaf.GeneAnnotation;

public interface SeedingDataProvider {

	public Map<Bioentity, List<GeneAnnotation>> getGeneProducts(String bp)
			throws IOException;

	public Map<Bioentity, List<GeneAnnotation>> getFunctions(
			Set<Bioentity> entities) throws IOException;

	public Map<Bioentity, List<GeneAnnotation>> getLocations(
			Set<Bioentity> entities) throws IOException;

}
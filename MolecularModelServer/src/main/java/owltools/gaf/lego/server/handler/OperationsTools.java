package owltools.gaf.lego.server.handler;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLNamedObject;

import owltools.gaf.lego.json.MolecularModelJsonRenderer;
import owltools.gaf.lego.server.external.ExternalLookupService;
import owltools.gaf.lego.server.external.ExternalLookupService.LookupEntry;
import owltools.util.ModelContainer;

class OperationsTools {

	static void requireNotNull(Object value, String msg) throws MissingParameterException {
		if (value == null) {
			throw new MissingParameterException("Expected non-null value for: "+msg);
		}
	}
	
	
	static class MissingParameterException extends Exception {

		private static final long serialVersionUID = 4362299465121954598L;

		/**
		 * @param message
		 */
		MissingParameterException(String message) {
			super(message);
		}
		
	}

	/**
	 * Normalize the userId.
	 * 
	 * @param userId
	 * @return normalized id or null
	 */
	static String normalizeUserId(String userId) {
		if (userId != null) {
			userId = StringUtils.trimToNull(userId);
			// quick hack, may be removed once all users are required to have a user id.
			if ("anonymous".equalsIgnoreCase(userId)) {
				return null;
			}
		}
		return userId;
	}
	
	/**
	 * @param model
	 * @param externalLookupService
	 * @return renderer
	 */
	static MolecularModelJsonRenderer createModelRenderer(
			final ModelContainer model, 
			final ExternalLookupService externalLookupService) {
		
		MolecularModelJsonRenderer renderer;
		if (externalLookupService != null) {
			renderer = new MolecularModelJsonRenderer(model.getAboxOntology()) {

				@Override
				protected String getLabel(OWLNamedObject i, String id) {
					String label = super.getLabel(i, id);
					if (label == null ) {
						// TODO get taxon for now take the first one
						// externalLookupService.lookup(id, taxon);
						List<LookupEntry> lookup = externalLookupService.lookup(id);
						if (lookup != null && !lookup.isEmpty()) {
							LookupEntry entry = lookup.iterator().next();
							label = entry.label;
						}
					}
					return label;
				}

			};
		}
		else {
			renderer = new MolecularModelJsonRenderer(model.getAboxOntology());
		}
		return renderer;
	}
}

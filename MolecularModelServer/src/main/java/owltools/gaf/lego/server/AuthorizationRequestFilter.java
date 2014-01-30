package owltools.gaf.lego.server;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

public class AuthorizationRequestFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		UriInfo uriInfo = requestContext.getUriInfo();
		List<String> matchedURIs = uriInfo.getMatchedURIs();
		if (matchedURIs.contains("m3StoreModel")) {
			boolean hasSecurityToken = hasSecurityToken(requestContext);
			if (!hasSecurityToken) {
				abort(requestContext);
			}
		}
	}

	private void abort(ContainerRequestContext requestContext) {
		ResponseBuilder builder = Response.status(Response.Status.UNAUTHORIZED);
		builder = builder.entity("User cannot access the resource.");
		requestContext.abortWith(builder.build());
	}
	
	private boolean hasSecurityToken(ContainerRequestContext requestContext) {
		UriInfo uriInfo = requestContext.getUriInfo();
		MultivaluedMap<String,String> queryParameters = uriInfo.getQueryParameters();
		String secToken = queryParameters.getFirst("security-token");
		Set<String> required = getCurrentSecurityTokens();
		boolean hasToken = required.contains(secToken);
		return hasToken;
	}
	
	private Set<String> getCurrentSecurityTokens() {
		// TODO
		return Collections.singleton("0815");
	}
}

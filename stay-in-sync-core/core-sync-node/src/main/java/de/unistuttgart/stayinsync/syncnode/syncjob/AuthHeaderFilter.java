package de.unistuttgart.stayinsync.syncnode.syncjob;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@ApplicationScoped
public class AuthHeaderFilter implements ClientRequestFilter {
    // @Inject
    // KeyVaultService keyVaultService; // Service Class for ApiKeys

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        // TODO: Connect to service
        // String authKeyAlias = getCurrentTransformationAuthKey();
        // String token = keyVaultService.getToken(authKeyAlias);

        String token = "dummy-bearer-token-12345"; // TODO: REMOVE PLACEHOLDER

        requestContext.getHeaders().add("Authorization", "Bearer " + token);
    }
}

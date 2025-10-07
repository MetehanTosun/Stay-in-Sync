package de.unistuttgart.stayinsync.core.configuration.service.aas;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Map;

@ApplicationScoped
public class AasClientHeadersFactory implements ClientHeadersFactory {
    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        if (clientOutgoingHeaders != null) {
            result.putAll(clientOutgoingHeaders);
        }
        for (Map.Entry<String, String> e : RequestHeadersContext.get().entrySet()) {
            result.putSingle(e.getKey(), e.getValue());
        }
        return result;
    }
}

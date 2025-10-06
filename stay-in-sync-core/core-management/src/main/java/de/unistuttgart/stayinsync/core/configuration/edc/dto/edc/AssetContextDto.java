package de.unistuttgart.stayinsync.core.configuration.edc.dto.edc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DataTransferObject for Edc communication that sets the context in which an asset is created.
 * The Context is always set to the eclipse tractus-x standard.
 */
public record AssetContextDto(
        @JsonProperty("@vocab")
        String vocab,
        String edc,
        String tx,
        @JsonProperty("tx-auth")
        String txAuth,
        @JsonProperty("cx-policy")
        String cxPolicy,
        String dcat,
        String odrl
) {
    public AssetContextDto() {
        this(
                "https://w3id.org/edc/v0.0.1/ns/",
                "https://w3id.org/edc/v0.0.1/ns/",
                "https://w3id.org/tractusx/v0.0.1/ns/",
                "https://w3id.org/tractusx/auth/",
                "https://w3id.org/catenax/policy/",
                "https://www.w3.org/ns/dcat#",
                "http://www.w3.org/ns/odrl/2/"
        );
    }
}

package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * DTO zur Erstellung einer Contract Definition im EDC.
 * Diese Klasse repräsentiert die Datenstruktur, die zum Anlegen einer Contract Definition
 * im Eclipse Dataspace Connector (EDC) benötigt wird.
 */
public class CreateEDCContractDefinitionDTO {

    @JsonProperty("@id")
    String id;

    @JsonProperty("@type")
    String type = "ContractDefinition";

    @JsonProperty("@context")
    ContextDTO context = new ContextDTO();

    @JsonProperty("accessPolicyId")
    String accessPolicyId;

    @JsonProperty("contractPolicyId")
    String contractPolicyId;

    @JsonProperty("assetId")
    String assetId;

    String rawJson;

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ContextDTO getContext() {
        return context;
    }

    public void setContext(ContextDTO context) {
        this.context = context;
    }

    public String getAccessPolicyId() {
        return accessPolicyId;
    }

    public void setAccessPolicyId(String accessPolicyId) {
        this.accessPolicyId = accessPolicyId;
    }

    public String getContractPolicyId() {
        return contractPolicyId;
    }

    public void setContractPolicyId(String contractPolicyId) {
        this.contractPolicyId = contractPolicyId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }
}
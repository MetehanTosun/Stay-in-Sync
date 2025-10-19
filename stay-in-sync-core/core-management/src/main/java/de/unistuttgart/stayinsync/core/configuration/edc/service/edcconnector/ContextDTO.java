package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContextDTO {
    @JsonProperty("@vocab")
    String vocab = "https://w3id.org/edc/v0.0.1/ns/";
    String edc = "https://w3id.org/edc/v0.0.1/ns/";
    String tx = "https://w3id.org/tractusx/v0.0.1/ns/";
    @JsonProperty("@tx-auth")
    String txAuth = "https://w3id.org/tractusx/auth/";
    @JsonProperty("@cx-policy")
    String cxPolicy = "https://w3id.org/catenax/policy/";

    public String getVocab() {
        return vocab;
    }

    public void setVocab(String vocab) {
        this.vocab = vocab;
    }

    public String getEdc() {
        return edc;
    }

    public void setEdc(String edc) {
        this.edc = edc;
    }

    public String getTx() {
        return tx;
    }

    public void setTx(String tx) {
        this.tx = tx;
    }

    public String getTxAuth() {
        return txAuth;
    }

    public void setTxAuth(String txAuth) {
        this.txAuth = txAuth;
    }

    public String getCxPolicy() {
        return cxPolicy;
    }

    public void setCxPolicy(String cxPolicy) {
        this.cxPolicy = cxPolicy;
    }

    public String getOdrl() {
        return odrl;
    }

    public void setOdrl(String odrl) {
        this.odrl = odrl;
    }

    String odrl = "http://www.w3.org/ns/odrl/2/";

}

package de.evoila.cf.broker.model;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by reneschollmeyer, evoila on 14.03.18.
 */
public class NsLookupRequest {

    @JsonProperty("domains")
    private String domains;

    @JsonProperty("email")
    private String email;

    @JsonProperty("acceptTerms")
    private boolean acceptTerms;

    public String getDomains() {
        return domains;
    }

    public void setDomains(String domains) {
        this.domains = domains;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAcceptTerms() {
        return acceptTerms;
    }

    public void setAcceptTerms(boolean acceptTerms) {
        this.acceptTerms = acceptTerms;
    }
}

package de.evoila.cf.broker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by reneschollmeyer, evoila on 19.09.17.
 */
public class CertificateData {

    @JsonProperty("certificate")
    private String certificate;

    @JsonProperty("private_key")
    private String privateKey;

    public String getCertificate() {
        return certificate;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}


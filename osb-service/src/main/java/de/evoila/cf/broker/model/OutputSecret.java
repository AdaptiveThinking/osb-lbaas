package de.evoila.cf.broker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.openstack4j.model.barbican.Secret;

import java.util.Date;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 13.09.17.
 */
public class OutputSecret {

    private static final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";
    private static final String TIMEZONE = "UTC";

    private Secret secret;

    public OutputSecret(Secret secret) { this.secret = secret; }

    public String getStatus() { return secret.getStatus(); }

    @JsonFormat(pattern = DATE_FORMAT, timezone = TIMEZONE)
    public String getCreateTime() { return secret.getCreateTime(); }

    @JsonFormat(pattern = DATE_FORMAT, timezone = TIMEZONE)
    public String getUpdateTime() { return secret.getUpdateTime(); }

    @JsonFormat(pattern = DATE_FORMAT, timezone = TIMEZONE)
    public String getExpiration() { return secret.getExpiration(); }

    public String getAlgorithm() { return secret.getAlgorithm(); }

    public Integer getBitLength() { return secret.getBitLength(); }

    public String getMode() { return secret.getMode(); }

    public String getName() { return secret.getName(); }

    public String getSecretReference() { return secret.getSecretReference(); }

    public String getSecretType() { return  secret.getSecretType(); }

    public Map<String, String> getContentTypes() { return secret.getContentTypes(); }
}

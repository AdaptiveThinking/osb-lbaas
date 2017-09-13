package de.evoila.cf.broker.controller;

import de.evoila.cf.broker.bean.LbaaSBean;
import de.evoila.cf.broker.model.OutputSecret;
import de.evoila.cf.cpi.openstack.fluent.connection.OpenstackConnectionFactory;
import net.minidev.json.JSONObject;
import org.openstack4j.model.barbican.Secret;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.barbican.domain.BarbicanSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created by reneschollmeyer, evoila on 06.09.17.
 */
@RestController
@RequestMapping(value = "/v2")
@ConditionalOnBean(LbaaSBean.class)
public class LBaaSSecretController {
    private final Logger log = LoggerFactory.getLogger(LBaaSSecretController.class);

    public static final String SECRETS_BASE_PATH = "/v2/secrets";

    @RequestMapping(value = "/secrets", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createSecret(@RequestBody BarbicanSecret secretBody) {
        log.debug("POST: " + SECRETS_BASE_PATH);

        Secret secret = OpenstackConnectionFactory.connection()
                .barbican()
                .secrets()
                .create(secretBody);

        if (secret != null) {
            JSONObject secretRef = new JSONObject();
            secretRef.put("secret_ref", secret.getSecretReference());
            return new ResponseEntity<>(secretRef.toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("{}", HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/secrets/{uuid}", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OutputSecret> getSecret(@PathVariable("uuid") String uuid) {
        log.debug("GET: " + SECRETS_BASE_PATH + "/{uuid}, uuid = " + uuid);

        Secret secret = OpenstackConnectionFactory.connection()
                .barbican()
                .secrets()
                .get(uuid);

        if (secret != null) {
            return new ResponseEntity<>(new OutputSecret(secret), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/secrets/{uuid}/payload", method = RequestMethod.GET,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getPayload(@PathVariable("uuid") String uuid) {
        log.debug("GET: " + SECRETS_BASE_PATH + "/{uuid}/payload, uuid = " + uuid);

        String payload = OpenstackConnectionFactory.connection()
                .barbican()
                .secrets()
                .getPayload(uuid);

        if(!payload.isEmpty()) {
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("{}", HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/secrets/{uuid}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteSecret(@PathVariable("uuid") String uuid) {
        log.debug("DELETE: " + SECRETS_BASE_PATH + "/{uuid}, uuid = " + uuid);

        ActionResponse response = OpenstackConnectionFactory.connection()
                .barbican()
                .secrets()
                .delete(uuid);

        if (response.isSuccess()) {
            return new ResponseEntity<>("{}", HttpStatus.valueOf(response.getCode()));
        } else {
            return new ResponseEntity<>("{}", HttpStatus.valueOf(response.getCode()));
        }
    }
}

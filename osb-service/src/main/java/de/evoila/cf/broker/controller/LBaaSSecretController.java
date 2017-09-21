package de.evoila.cf.broker.controller;

import de.evoila.cf.broker.bean.LbaaSBean;
import de.evoila.cf.broker.model.CertificateData;
import de.evoila.cf.cpi.openstack.fluent.HeatFluent;
import de.evoila.cf.cpi.openstack.fluent.connection.OpenstackConnectionFactory;
import org.openstack4j.api.Builders;
import org.openstack4j.model.barbican.Container;
import org.openstack4j.model.barbican.ContainerSecret;
import org.openstack4j.model.barbican.Secret;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.ws.http.HTTPException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 06.09.17.
 */
@RestController
@RequestMapping(value = "/v2")
@ConditionalOnBean(LbaaSBean.class)
public class LBaaSSecretController {
    private final Logger log = LoggerFactory.getLogger(LBaaSSecretController.class);

    public static final String SECRETS_BASE_PATH = "/v2/secrets";

    public static final String CERTIFICATE_NAME_TEMPLATE = "certificate-%s";
    public static final String PRIVATE_KEY_NAME_TEMPLATE = "private-key-%s";
    public static final String CONTAINER_NAME_TEMPLATE = "container-%s";

    @Autowired
    private HeatFluent heatFluent;

    @PostMapping(value = "/manage/service_instances/{instanceId}/certs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> storeCertificate(@PathVariable("instanceId") String instanceId,
                                                   @RequestBody CertificateData data) {

        log.debug("Creating secret " + String.format(CERTIFICATE_NAME_TEMPLATE, instanceId));

        Secret certificate = createSecret(Builders.secret()
                .name(String.format(CERTIFICATE_NAME_TEMPLATE, instanceId))
                .payload(data.getCertificate())
                .payloadContentType(MediaType.TEXT_PLAIN_VALUE)
                .build());

        log.debug("Creating secret " + String.format(PRIVATE_KEY_NAME_TEMPLATE, instanceId));

        Secret privateKey = createSecret(Builders.secret()
                .name(String.format(PRIVATE_KEY_NAME_TEMPLATE, instanceId))
                .payload(data.getPrivateKey())
                .payloadContentType(MediaType.TEXT_PLAIN_VALUE)
                .build());

        List<ContainerSecret> secretReferences = new ArrayList<>();
        secretReferences.add(Builders.containerSecret()
                .name("certificate")
                .reference(certificate.getSecretReference())
                .build());
        secretReferences.add(Builders.containerSecret()
                .name("private_key")
                .reference(privateKey.getSecretReference())
                .build());

        log.debug("Creating container " + String.format(CONTAINER_NAME_TEMPLATE, instanceId));

        Container container = createContainer(Builders.container()
                .name(String.format(CONTAINER_NAME_TEMPLATE, instanceId))
                .secretReferences(secretReferences)
                .type("certificate")
                .build());

        String listenerId = getListenerId(instanceId);

        OpenstackConnectionFactory.connection().networking()
                .lbaasV2().listener()
                .update(listenerId, Builders.listenerV2Update()
                .defaultTlsContainerRef(container.getContainerReference())
                .build());

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping(value = "/manage/service_instances/{instanceId}/certs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateCertificate(@PathVariable("instanceId") String instanceId,
                                                    @RequestBody CertificateData data) {

        String listenerId = getListenerId(instanceId);

        String containerId = getUUID(OpenstackConnectionFactory.connection()
                .networking().lbaasV2().listener().get(listenerId).getDefaultTlsContainerRef());

        Container container = OpenstackConnectionFactory.connection()
                .barbican()
                .containers()
                .get(containerId);

        for(ContainerSecret containerSecret : container.getSecretReferences()) {
            log.debug("Deleting Secret " + containerSecret.getReference());

            ActionResponse deleteResponse = deleteSecret(getUUID(containerSecret.getReference()));
            if(deleteResponse.getCode() != HttpStatus.NO_CONTENT.value())
                throw new HTTPException(deleteResponse.getCode());
        }

        log.debug("Deleting Container " + container.getContainerReference());

        ActionResponse deleteContainer = deleteContainer(containerId);

        if(deleteContainer.getCode() != HttpStatus.NO_CONTENT.value())
            throw new HTTPException(deleteContainer.getCode());

        return storeCertificate(instanceId, data);
    }

    private Secret createSecret(Secret secret) {
        return OpenstackConnectionFactory.connection()
                .barbican()
                .secrets()
                .create(secret);
    }

    private Container createContainer(Container container) {
        return OpenstackConnectionFactory.connection()
                .barbican()
                .containers()
                .create(container);
    }

    private ActionResponse deleteSecret(String secretId) {
        return OpenstackConnectionFactory.connection()
                .barbican()
                .secrets()
                .delete(secretId);
    }

    private ActionResponse deleteContainer(String containerId) {
        return OpenstackConnectionFactory.connection()
                .barbican()
                .containers()
                .delete(containerId);
    }

    private String getListenerId(String instanceId) {
        Stack lbaas = heatFluent.get("lbaas-" + instanceId);

        return OpenstackConnectionFactory.connection().networking()
                .lbaasV2().loadbalancer().get(getLoadbalancerId(lbaas.getOutputs()))
                .getListeners().get(0).getId();
    }

    private String getLoadbalancerId(List<Map<String, Object>> outputs) {
        for(Map<String, Object> map : outputs) {
            if(map.get("output_key").equals("loadbalancer"))
                return map.get("output_value").toString();
        }

        return null;
    }

    private String getUUID(String reference) {
        return reference.substring(reference.lastIndexOf("/") + 1);
    }
}

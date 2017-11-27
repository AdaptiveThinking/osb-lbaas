package de.evoila.cf.broker.controller;

import de.evoila.cf.broker.bean.LbaaSBean;
import de.evoila.cf.broker.exception.CertificateAlreadyExistsException;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.CertificateData;
import de.evoila.cf.broker.model.ErrorMessage;
import de.evoila.cf.broker.persistence.mongodb.repository.ServiceStackMapping;
import de.evoila.cf.broker.persistence.mongodb.repository.StackMappingRepository;
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

    @Autowired
    private StackMappingRepository stackMappingRepository;

    @PostMapping(value = "/manage/service_instances/{instanceId}/certs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> storeCertificate(@PathVariable("instanceId") String instanceId,
                                                   @RequestBody CertificateData data) throws ServiceInstanceDoesNotExistException, CertificateAlreadyExistsException {

        log.debug("POST: " + SECRETS_BASE_PATH + "/manage/service_instances/{instanceId}/certs," +
                "storeCertificate(), serviceInstanceId = " + instanceId);

        ServiceStackMapping stackMapping = stackMappingRepository.findOne(instanceId);

        if(stackMapping == null)
            throw new ServiceInstanceDoesNotExistException(instanceId);

        if(stackMapping.getCertified() == true)
            throw new CertificateAlreadyExistsException(instanceId);

        String listenerId = getListenerId(instanceId);

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

        OpenstackConnectionFactory.connection().networking()
                .lbaasV2().listener()
                .update(listenerId, Builders.listenerV2Update()
                .defaultTlsContainerRef(container.getContainerReference())
                .build());

        stackMapping.setCertified(true);
        stackMappingRepository.save(stackMapping);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping(value = "/manage/service_instances/{instanceId}/certs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateCertificate(@PathVariable("instanceId") String instanceId,
                                                    @RequestBody CertificateData data) throws ServiceInstanceDoesNotExistException, CertificateAlreadyExistsException {

        log.debug("PATCH: " + SECRETS_BASE_PATH + "/manage/service_instances/{instanceId}/certs," +
                "updateCertificate(), serviceInstanceId = " + instanceId);

        deleteCertificate(instanceId);

        return storeCertificate(instanceId, data);
    }

    @DeleteMapping(value = "/manage/service_instances/{instanceId}/certs")
    public ResponseEntity<String> deleteCertificate(@PathVariable("instanceId") String instanceId) throws ServiceInstanceDoesNotExistException {

        log.debug("DELETE: " + SECRETS_BASE_PATH + "/manage/service_instances/{instanceId}/certs," +
                "deleteCertificate(), serviceInstanceId = " + instanceId);

        ServiceStackMapping stackMapping = stackMappingRepository.findOne(instanceId);

        String listenerId = getListenerId(instanceId);

        if(listenerId != null) {

            String containerReference = OpenstackConnectionFactory.connection()
                    .networking().lbaasV2().listener().get(listenerId).getDefaultTlsContainerRef();

            if (containerReference != null) {
                String containerId = getUUID(containerReference);

                OpenstackConnectionFactory.connection()
                        .networking().lbaasV2().listener().update(listenerId, Builders.listenerV2Update()
                        .defaultTlsContainerRef("")
                        .build());

                Container container = OpenstackConnectionFactory.connection()
                        .barbican()
                        .containers()
                        .get(containerId);

                for (ContainerSecret containerSecret : container.getSecretReferences()) {
                    log.debug("Deleting Secret " + containerSecret.getReference());

                    ActionResponse deleteResponse = deleteSecret(getUUID(containerSecret.getReference()));
                    if (deleteResponse.getCode() != HttpStatus.NO_CONTENT.value())
                        throw new HTTPException(deleteResponse.getCode());
                }

                log.debug("Deleting Container " + container.getContainerReference());

                ActionResponse deleteContainer = deleteContainer(containerId);

                stackMapping.setCertified(false);
                stackMappingRepository.save(stackMapping);

                return new ResponseEntity<>("{}", HttpStatus.valueOf(deleteContainer.getCode()));
            }
        }

        return new ResponseEntity<>("{}", HttpStatus.NO_CONTENT);
    }

    @GetMapping(value = "/manage/service_instances/{instanceId}/certs")
    public ResponseEntity<Boolean> isCertified(@PathVariable("instanceId") String instanceId) {
        if(stackMappingRepository.findOne(instanceId).getCertified()) {
            return new ResponseEntity(true, HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping(value = "/manage/service_instances/{instanceId}/fip", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> publicIp(@PathVariable("instanceId") String instanceId) throws ServiceInstanceDoesNotExistException {
        ServiceStackMapping stackMapping = stackMappingRepository.findOne(instanceId);

        if(stackMapping == null)
            throw new ServiceInstanceDoesNotExistException(instanceId);

        Stack lbaas = heatFluent.get("lbaas-" + instanceId);

        if(lbaas == null) {
            return new ResponseEntity<String>("Loadbalancer not found", HttpStatus.BAD_REQUEST);
        }

        String publicIp = getPublicIp(lbaas.getOutputs());

        return new ResponseEntity<String>("{ \"publicIp\" :" + "\"" + publicIp + "\" }", HttpStatus.OK);
    }

    @ExceptionHandler(ServiceInstanceDoesNotExistException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessage> handleException(ServiceInstanceDoesNotExistException ex) {
        return new ResponseEntity<ErrorMessage>(new ErrorMessage(ex.getMessage()), HttpStatus.GONE);
    }

    @ExceptionHandler(CertificateAlreadyExistsException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessage> handleException(CertificateAlreadyExistsException ex) {
        return new ResponseEntity<ErrorMessage>(new ErrorMessage(ex.getMessage()), HttpStatus.BAD_REQUEST);
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

    private String getListenerId(String instanceId) throws ServiceInstanceDoesNotExistException {
        Stack lbaas = heatFluent.get("lbaas-" + instanceId);

        if(lbaas == null) {
            return null;
        }

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

    private String getPublicIp(List<Map<String, Object>> outputs) {
        for(Map<String, Object> map : outputs) {
            if(map.get("output_key").equals("fip_address"))
                return map.get("output_value").toString();
        }

        return null;
    }

    private String getUUID(String reference) {
        return reference.substring(reference.lastIndexOf("/") + 1);
    }
}

package de.evoila.cf.broker.controller;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.exception.ServiceDefinitionDoesNotExistException;
import de.evoila.cf.broker.model.CertificateData;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.persistence.mongodb.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.cpi.bosh.LbaaSBoshPlatformService;
import de.evoila.cf.cpi.bosh.LbaaSDeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 06.09.17.
 */
@RestController
@RequestMapping(value = "/v2")
public class LBaaSSecretController {

    private final Logger log = LoggerFactory.getLogger(LBaaSSecretController.class);

    public static final String SECRETS_BASE_PATH = "/v2/secrets";

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Autowired
    private DeploymentManager deploymentManager;

    @Autowired
    private BoshProperties boshProperties;

    @Autowired
    private LbaaSBoshPlatformService lbaaSBoshPlatformService;

    @PatchMapping(value = "/manage/service_instances/{instanceId}/certs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> storeCertificate(@PathVariable("instanceId") String instanceId,
                                                   @RequestBody CertificateData data) {

        log.debug("POST: " + SECRETS_BASE_PATH + "/manage/service_instances/{instanceId}/certs," +
                "storeCertificate(), serviceInstanceId = " + instanceId);

        try {
            updateDeployment(instanceId, data);
        } catch (ServiceDefinitionDoesNotExistException | PlatformException e) {
            return new ResponseEntity<>("{ \"message\": " + e.getMessage() + " }", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("{ \"message\": \"OK\"}", HttpStatus.OK);
    }

    private void updateDeployment(String instanceId, CertificateData data) throws PlatformException,
            ServiceDefinitionDoesNotExistException {
        ServiceInstance instance = serviceInstanceRepository.findById(instanceId).orElse(null);
        Plan plan = serviceDefinitionRepository.getPlan(instance.getPlanId());

        String certificates = data.getCertificate();
        certificates += "\n";
        certificates += data.getPrivateKey();

        Map<String, Object> sslPem = new HashMap<>();
        sslPem.put(LbaaSDeploymentManager.SSL_PEM, certificates);

        lbaaSBoshPlatformService.updateInstance(instance, plan, sslPem);
    }
}
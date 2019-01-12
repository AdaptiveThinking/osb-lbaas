package de.evoila.cf.broker.controller;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.exception.ServiceDefinitionDoesNotExistException;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.NsLookupResponse;
import de.evoila.cf.broker.model.ResponseMessage;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.service.DeploymentService;
import de.evoila.cf.broker.service.custom.LbaaSService;
import de.evoila.cf.broker.util.ParameterValidator;
import de.evoila.cf.cpi.bosh.LbaaSBoshPlatformService;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Rene Schollmeyer, Johannes Hiemer.
 */
@RestController
@RequestMapping(value = "/custom/v2/manage/service_instances")
public class LBaasController {

    private final Logger log = LoggerFactory.getLogger(LBaasController.class);

    private static String INSTANCE_GROUP = "ha_proxy";

    private ServiceInstanceRepository serviceInstanceRepository;

    private ServiceDefinitionRepository serviceDefinitionRepository;

    private DeploymentService deploymentService;

    private LbaaSService lbaaSService;

    private BoshProperties boshProperties;

    private LbaaSBoshPlatformService lbaaSBoshPlatformService;

    public LBaasController(ServiceInstanceRepository serviceInstanceRepository,
                           ServiceDefinitionRepository serviceDefinitionRepository,
                           DeploymentService deploymentService,
                           LbaaSService lbaaSService,
                           BoshProperties boshProperties,
                           LbaaSBoshPlatformService lbaaSBoshPlatformService) {
        this.serviceInstanceRepository = serviceInstanceRepository;
        this.serviceDefinitionRepository = serviceDefinitionRepository;
        this.deploymentService = deploymentService;
        this.lbaaSService = lbaaSService;
        this.boshProperties = boshProperties;
        this.lbaaSBoshPlatformService = lbaaSBoshPlatformService;
    }

    @GetMapping(value = "/{serviceInstanceId}/fip")
    public ResponseEntity<Map> fip(@PathVariable("serviceInstanceId") String serviceInstanceId) throws ServiceInstanceDoesNotExistException,
            ServiceBrokerException {

        ServiceInstance serviceInstance = serviceInstanceRepository.getServiceInstance(serviceInstanceId);
        if (serviceInstance == null)
            throw new ServiceInstanceDoesNotExistException("Could not find Service Instance");

        Manifest manifest;
        try {
            manifest = lbaaSBoshPlatformService.getDeployedManifest(serviceInstance);
        } catch (IOException ex) {
            throw new ServiceBrokerException("Cannot read HaProxy configuration", ex);
        }

        if (manifest != null) {
            String publicIp = lbaaSService.getPublicIp(manifest, boshProperties.getVipNetwork());

            Map response = Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("publicIp", publicIp);
            }});

            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PatchMapping(value = "/{serviceInstanceId}/validate")
    public ResponseEntity validate(@PathVariable("serviceInstanceId") String serviceInstanceId,
                                   @RequestBody Map<String, Object> request) throws ServiceInstanceDoesNotExistException,
            ServiceBrokerException, ServiceDefinitionDoesNotExistException, ValidationException {

        ServiceInstance serviceInstance = serviceInstanceRepository.getServiceInstance(serviceInstanceId);
        if (serviceInstance == null)
            throw new ServiceInstanceDoesNotExistException("Could not find Service Instance");

        Plan plan = serviceDefinitionRepository.getPlan(serviceInstance.getPlanId());

        ParameterValidator.validateParameters(request, plan, true);

        Manifest manifest = null;
        try {
            manifest = lbaaSBoshPlatformService.getDeployedManifest(serviceInstance);
        } catch (IOException ex) {
            throw new ServiceBrokerException("Cannot read HaProxy configuration", ex);
        }

        if (manifest != null) {
            NsLookupResponse response = new NsLookupResponse();

            String publicIp = lbaaSService.getPublicIp(manifest, boshProperties.getVipNetwork());

            Map<String, Object> haProxySection = (Map<String, Object>) request.get(INSTANCE_GROUP);
            Map<String, Object> letsEncryptSection = (Map<String, Object>) haProxySection.get("letsencrypt");
            for (String domain : (List<String>) letsEncryptSection.get("domains")) {
                if (!nslookup(domain, publicIp)) {
                    response.addFalseResult(domain);
                }
            }

            if (response.getFalseResults().isEmpty()) {
                return new ResponseEntity<>(new ResponseMessage<>("All domains validated"), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new ResponseMessage<>(response.getFalseResults()), HttpStatus.ACCEPTED);
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    private boolean nslookup(String host, String ip) {
        boolean result = false;

        try {
            if (InetAddress.getByName(host.trim()).getHostAddress().equals(ip))
                result = true;
        } catch (UnknownHostException e) {}

        return result;
    }

}

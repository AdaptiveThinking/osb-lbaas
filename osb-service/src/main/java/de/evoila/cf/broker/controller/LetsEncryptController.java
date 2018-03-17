package de.evoila.cf.broker.controller;

import com.google.common.base.Splitter;
import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.persistence.mongodb.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.cpi.bosh.LbaaSBoshPlatformService;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import io.bosh.client.deployments.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by reneschollmeyer, evoila on 13.03.18.
 */
@RestController
@RequestMapping(value = "/v2/manage/service_instances")
public class LetsEncryptController  {

    private final Logger log = LoggerFactory.getLogger(LetsEncryptController.class);

    private static String INSTANCE_GROUP = "haproxy";

    @Autowired
    private LbaaSBoshPlatformService lbaaSBoshPlatformService;

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Autowired
    private DeploymentManager deploymentManager;

    @Autowired
    private BoshProperties boshProperties;

    @PostMapping(value = "/{instanceId}/dns", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity letsEncrypt(@PathVariable("instanceId") String instanceId,
                                      @RequestBody NsLookupRequest request) throws  ServiceBrokerException, IOException, PlatformException {

        List<String> domainList = Splitter.on(",").splitToList(request.getDomains());

        NsLookupResponse response = new NsLookupResponse();

        String fip = publicIp(instanceId).getBody().get("publicIp").toString();

        for(String domain : domainList) {
            if(!nslookup(domain, fip)) {
                response.getFalseResults().get("message").add(domain);
            }
        }

        if(response.getFalseResults().get("message").isEmpty()) {
            updateDeployment(instanceId, request, domainList);
            return new ResponseEntity<>("{ \"message\": \"OK\"}", HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response.getFalseResults(), HttpStatus.ACCEPTED);
        }
    }

    @GetMapping(value = "/{instanceId}/fip")
    public ResponseEntity<Map> publicIp(@PathVariable("instanceId") String instanceId) throws IOException {
        String publicIp = "";

        Deployment deployment = lbaaSBoshPlatformService.getConnection()
                .connection()
                .deployments()
                .get("sb-" + instanceId).toBlocking().first();

        Manifest manifest = deploymentManager.getMapper().readValue(deployment.getRawManifest(), Manifest.class);

        List<NetworkReference> networks = manifest.getInstance_groups()
                .stream()
                .filter(i -> i.getName().equals(INSTANCE_GROUP))
                .findAny().get().getNetworks();

        for(NetworkReference network : networks) {
            if(network.getName().equals(boshProperties.getVipNetwork())) {
                publicIp = network.getStaticIps().get(0);
            }
        }

        if(publicIp.isEmpty()) {
            publicIp = "No public ip specified";
        }

        Map<String, String> response = new HashMap<>();
        response.put("publicIp", publicIp);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private boolean nslookup(String host, String ip) {
        try {
            if(InetAddress.getByName(host.trim()).getHostAddress().equals(ip)) {
                return true;
            }
        } catch (UnknownHostException e) {
            return false;
        }

        return false;
    }

    private void updateDeployment(String instanceId, NsLookupRequest request, List<String> domainList) throws ServiceBrokerException, IOException, PlatformException {
        ServiceInstance instance = serviceInstanceRepository.findOne(instanceId);
        Plan plan = serviceDefinitionRepository.getPlan(instance.getPlanId());

        Map<String, Object> letsencrypt = new HashMap<>();
        letsencrypt.put("enabled", true);
        letsencrypt.put("email", request.getEmail().trim());

        domainList.stream()
                .map(d -> d.trim())
                .collect(Collectors.toList());

        letsencrypt.put("domains", domainList);

        plan.getMetadata().getCustomParameters().put("letsencrypt", letsencrypt);

        lbaaSBoshPlatformService.updateInstance(instance, plan);
    }
}

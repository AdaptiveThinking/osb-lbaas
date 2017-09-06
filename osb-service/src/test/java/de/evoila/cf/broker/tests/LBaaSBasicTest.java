package de.evoila.cf.broker.tests;


import de.evoila.Application;
import de.evoila.cf.broker.controller.ServiceInstanceController;
import de.evoila.cf.broker.model.ServiceInstanceRequest;
import de.evoila.cf.broker.model.ServiceInstanceResponse;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by reneschollmeyer, evoila on 04.09.17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@ActiveProfiles("development")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LBaaSBasicTest {
    private static ServiceInstanceRequest serviceInstanceRequest;
    private static final String instanceId = "0b8eb32c-4ea7-4d1e-8b13-7a7fa3a02aa0";

    @Autowired
    public ServiceInstanceController serviceInstanceController;

    @Autowired
    public ServiceInstanceRepository serviceInstanceRepository;

    @PostConstruct
    public void init() {
        Map<String, String> context = new HashMap<>();
        serviceInstanceRequest = new ServiceInstanceRequest("lbaas-dev", "lbaas_s_dev",
                "organization_guid", "plan_guid", context);
        serviceInstanceRequest.setParameters(new HashMap<>());
    }

    @Test
    public void createInstance() throws Exception {
        ResponseEntity<ServiceInstanceResponse> responseEntity = serviceInstanceController.createServiceInstance(instanceId,
                true, serviceInstanceRequest);

        Thread.sleep(5000);

        while(serviceInstanceController.lastOperation(instanceId).getBody().getState().equals("in progress")) {
            Thread.sleep(5000);
        }

        assertNotNull(responseEntity);
        assertEquals(true, serviceInstanceRepository.containsServiceInstanceId(instanceId));
    }

    @Test
    public void deleteInstance() throws Exception {
        ResponseEntity<String> responseEntity = serviceInstanceController.deleteServiceInstance(instanceId,
                serviceInstanceRequest.getServiceDefinitionId(), serviceInstanceRequest.getPlanId());

        Thread.sleep(5000);

        assertNotNull(responseEntity);
        assertEquals(false, serviceInstanceRepository.containsServiceInstanceId(instanceId));
    }
}

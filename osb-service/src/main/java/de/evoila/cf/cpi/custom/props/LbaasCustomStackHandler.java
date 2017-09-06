package de.evoila.cf.cpi.custom.props;

import de.evoila.cf.broker.bean.LbaaSBean;
import de.evoila.cf.broker.bean.OpenstackBean;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.persistence.mongodb.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.persistence.mongodb.repository.ServiceStackMapping;
import de.evoila.cf.broker.persistence.mongodb.repository.StackMappingRepository;
import de.evoila.cf.cpi.openstack.custom.CustomStackHandler;
import org.apache.catalina.Server;
import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 25.08.17.
 */
@Service
@ConditionalOnBean(OpenstackBean.class)
public class LbaasCustomStackHandler extends CustomStackHandler {

    private static final String NAME_TEMPLATE = "lbaas-%s";

    private static final String MAIN_TEMPLATE = "/openstack/lbaas_1m.yaml";

    private static final Logger log = LoggerFactory.getLogger(LbaasCustomStackHandler.class);

    private String subnet;
    private int port;
    private List<String> addresses;
    private String public_network_id;

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private StackMappingRepository stackMappingRepo;

    @Autowired
    private OpenstackBean openstackBean;

    @Autowired
    private LbaaSBean lbaasBean;

    @PostConstruct
    public void initValues() {
        subnet = openstackBean.getSubnetId();
        public_network_id = openstackBean.getPublicNetworkId();
        port = lbaasBean.getPort();
        addresses = lbaasBean.getAddresses();
    }

    @Override
    public String create(String instanceId, Map<String, String> customParameters) throws PlatformException {
        ServiceStackMapping stackMapping = new ServiceStackMapping();
        stackMapping.setId(instanceId);

        log.debug(customParameters.toString());
        log.debug("Start creating LBaaS...");
        Stack lbaas = createInstance(instanceId, customParameters);
        stackMapping.setLbaasStack(lbaas.getId());
        log.debug("End creating LBaaS.");
        stackMappingRepo.save(stackMapping);

        return stackMapping.getId();
    }

    public Stack createInstance(String instanceId, Map<String, String> customParameters) throws PlatformException {
        String name = String.format(NAME_TEMPLATE, instanceId);

        Map<String, String> specificParameters = new HashMap<String, String>();

        specificParameters.put("name", name);
        specificParameters.put("subnet", subnet);
        specificParameters.put("port", String.valueOf(port));
        specificParameters.put("addresses", String.join(",", addresses));
        specificParameters.put("public_network_id", public_network_id);

        String template = accessTemplate(MAIN_TEMPLATE);

        heatFluent.create(name, template, specificParameters, false, 10l);

        Stack lbaasStack = stackProgressObserver.waitForStackCompletion(name);

        return lbaasStack;
    }

    @Override
    public void delete(String internalId) {
        ServiceStackMapping stackMapping = stackMappingRepo.findOne(internalId);

        if(stackMapping == null) {
            super.delete(internalId);
        } else {
            super.delete(stackMapping.getLbaasStack());
            stackMappingRepo.delete(stackMapping);
        }
    }
}

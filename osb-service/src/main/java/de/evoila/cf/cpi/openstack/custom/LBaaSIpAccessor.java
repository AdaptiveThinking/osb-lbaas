package de.evoila.cf.cpi.openstack.custom;

import com.google.common.collect.Lists;
import de.evoila.cf.broker.bean.LbaaSBean;
import de.evoila.cf.broker.bean.OpenstackBean;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.cpi.openstack.fluent.HeatFluent;
import org.openstack4j.model.heat.Stack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 04.09.17.
 */
@Service
@Primary
@ConditionalOnBean(HeatFluent.class)
public class LBaaSIpAccessor extends CustomIpAccessor {

    private static final String PUBLIC_FLOATING_IP = "fip_address";

    @Autowired
    private HeatFluent heatFluent;

    @Autowired
    private DefaultIpAccessor defaultIpAccessor;

    @Autowired
    private LbaaSBean lbaaSBean;

    @Override
    public List<ServerAddress> getIpAddresses(String instanceId) throws PlatformException {
        Stack stack = heatFluent.get("lbaas-" + instanceId);
        List<Map<String, Object>> outputs = stack.getOutputs();

        if(outputs == null || outputs.isEmpty()) {
            return defaultIpAccessor.getIpAddresses(instanceId);
        }

        List<ServerAddress> serverAddresses = Lists.newArrayList();
        for(Map<String, Object> output : outputs) {
            Object outputKey = output.get("output_key");
            if(outputKey != null && outputKey instanceof  String) {
                String key = (String) outputKey;
                    if(key.equals(PUBLIC_FLOATING_IP)) {
                        serverAddresses.add(new ServerAddress("fip_address", (String) output.get("output_value"), lbaaSBean.getPort()));
                    }
            }
        }

        return serverAddresses;
    }
}

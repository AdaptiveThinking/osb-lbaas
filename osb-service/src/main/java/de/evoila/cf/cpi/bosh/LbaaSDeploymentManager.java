package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.bean.OpenstackBean;
import de.evoila.cf.broker.model.NetworkReference;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import de.evoila.cf.cpi.openstack.fluent.connection.OpenstackConnectionFactory;
import org.openstack4j.model.compute.FloatingIP;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 12.03.18.
 */
@Service
public class LbaaSDeploymentManager extends DeploymentManager {
    public static final String INSTANCE_GROUP = "haproxy";
    public static final String DATA_PATH = "data_path";
    public static final String PORT = "port";

    private OpenstackBean openstackBean;

    public LbaaSDeploymentManager(BoshProperties boshProperties, OpenstackBean openstackBean) {
        super(boshProperties);
        this.openstackBean = openstackBean;
    }

    @Override
    protected void replaceParameters(ServiceInstance instance, Manifest manifest, Plan plan, Map<String, String> customParameters) {
        HashMap<String, Object> properties = new HashMap<>();
        //properties.putAll(plan.getMetadata());
        properties.putAll(customParameters);

        Map<String, Object> manifestProperties = manifest.getInstance_groups()
                .stream()
                .filter(i -> i.getName().equals(INSTANCE_GROUP))
                .findAny().get().getProperties();

        HashMap<String, Object> haproxy = (HashMap<String, Object>) manifestProperties.get("haproxy");

        if(plan.getMetadata() != null
                && plan.getMetadata().getCustomParameters() != null
                && plan.getMetadata().getCustomParameters().containsKey("letsencrypt")) {
            haproxy.put("letsencrypt", plan.getMetadata().getCustomParameters().get("letsencrypt"));
        }

        if(properties.containsKey(DATA_PATH)) {
            haproxy.put(DATA_PATH, properties.get(DATA_PATH));
        }

        if (properties.containsKey(PORT)) {
            haproxy.put(PORT, properties.get(PORT));
        }

        this.updateInstanceGroupConfiguration(manifest, plan);

        updateFloatingIp(manifest, instance);

    }

    public void updateFloatingIp(Manifest manifest, ServiceInstance instance) {
        List<NetworkReference> networks = manifest.getInstance_groups()
                .stream()
                .filter(i -> i.getName().equals(INSTANCE_GROUP))
                .findAny().get().getNetworks();

        for(NetworkReference network : networks) {
            if(network.getName().equals("floating") && network.getStaticIps().isEmpty()) {
                FloatingIP floatingIP = floatingIp(openstackBean.getPool());

                System.out.print("Found Static IP");
                network.getStaticIps().add(floatingIP.getFloatingIpAddress());
                instance.setFloatingIpId(floatingIP.getId());
            }
        }
    }

    public FloatingIP floatingIp(String pool) {
        FloatingIP floatingIP = OpenstackConnectionFactory.connection().compute().floatingIps().allocateIP(pool);

        return floatingIP;
    }
}

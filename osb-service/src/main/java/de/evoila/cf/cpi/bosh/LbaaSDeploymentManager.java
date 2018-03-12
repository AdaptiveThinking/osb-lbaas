package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 12.03.18.
 */
public class LbaaSDeploymentManager extends DeploymentManager {
    public static final String INSTANCE_GROUP = "haproxy";
    public static final String DATA_PATH = "data_path";
    public static final String PORT = "port";

    public LbaaSDeploymentManager(BoshProperties boshProperties) {
        super(boshProperties);
    }

    @Override
    protected void replaceParameters(ServiceInstance instance, Manifest manifest, Plan plan, Map<String, String> customParameters) {
        HashMap<String, Object> properties = new HashMap<>();
        properties.putAll(plan.getMetadata());
        properties.putAll(customParameters);

        Map<String, Object> manifestProperties = manifest.getInstance_groups()
                .stream()
                .filter(i -> i.getName().equals(INSTANCE_GROUP))
                .findAny().get().getProperties();

        HashMap<String, Object> haproxy = (HashMap<String, Object>) manifestProperties.get("haproxy");

        if(properties.containsKey(DATA_PATH)) {
            haproxy.put(DATA_PATH, properties.get(DATA_PATH));
        }

        if (properties.containsKey(PORT)) {
            haproxy.put(PORT, properties.get(PORT));
        }

        this.updateInstanceGroupConfiguration(manifest, plan);
    }
}

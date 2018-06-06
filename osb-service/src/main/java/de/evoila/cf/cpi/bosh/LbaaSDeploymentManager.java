package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.bean.OpenstackBean;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import de.evoila.cf.cpi.openstack.fluent.connection.OpenstackConnectionFactory;
import org.openstack4j.model.compute.FloatingIP;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by reneschollmeyer, evoila on 12.03.18.
 */
@Service
public class LbaaSDeploymentManager extends DeploymentManager {

    protected static final String INSTANCE_GROUP = "haproxy";
    protected static final String HA_PROXY_PROPERTIES = "ha_proxy";
    protected static final String DATA_PATH = "data_path";
    protected static final String PORT = "port";
    public static final String LETSENCRYPT = "letsencrypt";
    public static final String SSL_PEM = "ssl_pem";

    private OpenstackBean openstackBean;

    public LbaaSDeploymentManager(BoshProperties boshProperties, OpenstackBean openstackBean) {
        super(boshProperties);
        this.openstackBean = openstackBean;
    }

    @Override
    protected void replaceParameters(ServiceInstance instance, Manifest manifest, Plan plan, Map<String, Object> customParameters) {
        log.debug("Updating Deployment Manifest, replacing parameters");

        Map<String, Object> manifestProperties = manifest.getInstanceGroups()
                .stream()
                .filter(i -> i.getName().equals(INSTANCE_GROUP))
                .findAny().get().getProperties();

        HashMap<String, Object> haproxy = (HashMap<String, Object>) manifestProperties.get(HA_PROXY_PROPERTIES);

        if (customParameters != null) {
            if(customParameters.containsKey(LETSENCRYPT)) {
                haproxy.put(LETSENCRYPT, customParameters.get(LETSENCRYPT));

                if (haproxy.containsKey(SSL_PEM))
                    haproxy.remove(SSL_PEM);
            } else if(customParameters.containsKey(SSL_PEM)) {
                haproxy.put(SSL_PEM, customParameters.get(SSL_PEM));

                if (haproxy.containsKey(LETSENCRYPT))
                    haproxy.remove(LETSENCRYPT);
            }
        }

        if(customParameters.containsKey(DATA_PATH)) {
            haproxy.put(DATA_PATH, customParameters.get(DATA_PATH));
        }

        if (customParameters.containsKey(PORT)) {
            haproxy.put(PORT, customParameters.get(PORT));
        }

        updateInstanceGroupConfiguration(manifest, plan);
        updateFloatingIp(manifest, instance);
    }

    public void updateFloatingIp(Manifest manifest, ServiceInstance instance) {
        Assert.notNull(boshProperties.getVipNetwork(), "vip_network may not be null, when using LBaaS via Bosh");
        Assert.notNull(openstackBean.getPool(), "OpenStack Public IP Pool may not be null, when using LBaaS via Bosh");

        manifest.getInstanceGroups()
                .stream()
                .filter(i -> i.getName().equals(INSTANCE_GROUP))
                .findFirst().get().getNetworks()
                .stream()
                .map(n -> {
                    if(n.getName().equals(boshProperties.getVipNetwork()) && n.getStaticIps().isEmpty()) {
                        FloatingIP floatingIP = OpenstackConnectionFactory
                                .connection()
                                .compute()
                                .floatingIps().allocateIP(openstackBean.getPool());

                        n.getStaticIps().add(floatingIP.getFloatingIpAddress());
                        instance.setFloatingIpId(floatingIP.getId());
                    }
                    return n;
                }).collect(Collectors.toList());

    }

}

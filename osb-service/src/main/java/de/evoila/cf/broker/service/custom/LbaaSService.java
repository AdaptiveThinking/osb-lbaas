package de.evoila.cf.broker.service.custom;

import de.evoila.cf.broker.model.catalog.plan.NetworkReference;
import de.evoila.cf.cpi.bosh.LbaaSBoshPlatformService;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Johannes Hiemer.
 */
@Component
public class LbaaSService {

    private static String INSTANCE_GROUP = "ha_proxy";

    private LbaaSBoshPlatformService lbaaSBoshPlatformService;

    private DeploymentManager deploymentManager;

    public LbaaSService(LbaaSBoshPlatformService lbaaSBoshPlatformService,
                        DeploymentManager deploymentManager) {
        this.lbaaSBoshPlatformService = lbaaSBoshPlatformService;
        this.deploymentManager = deploymentManager;
    }

    public String getPublicIp(Manifest manifest, String vipNetworkname) {
        String publicIp = "No public IP specified";
        List<NetworkReference> networks = manifest.getInstanceGroups()
                .stream()
                .filter(i -> i.getName().equals(INSTANCE_GROUP))
                .findAny().get().getNetworks();

        for(NetworkReference network : networks) {
            if(network.getName().equals(vipNetworkname)) {
                publicIp = network.getStaticIps().get(0);
            }
        }

        return publicIp;
    }
}

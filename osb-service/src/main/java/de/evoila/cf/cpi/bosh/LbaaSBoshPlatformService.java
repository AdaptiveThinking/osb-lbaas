package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.bean.OpenstackBean;
import de.evoila.cf.broker.bean.SiteConfiguration;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.CatalogService;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.openstack.fluent.connection.OpenstackConnectionFactory;
import io.bosh.client.deployments.Deployment;
import io.bosh.client.errands.ErrandSummary;
import io.bosh.client.vms.Vm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by reneschollmeyer, evoila on 12.03.18.
 */
@Service
@ConditionalOnBean({ BoshProperties.class, OpenstackBean.class })
public class LbaaSBoshPlatformService extends BoshPlatformService {

    private static final int defaultPort = 80;

    public LbaaSBoshPlatformService(PlatformRepository repository, CatalogService catalogService,
                                    ServicePortAvailabilityVerifier availabilityVerifier,
                                    BoshProperties boshProperties,
                                    SiteConfiguration siteConfiguration,
                                    Optional<DashboardClient> dashboardClient,
                                    OpenstackBean openstackBean,
                                    Environment environment) {
        super(repository, catalogService,
                availabilityVerifier, boshProperties,
                dashboardClient, new LbaaSDeploymentManager(boshProperties, environment, openstackBean, siteConfiguration));
    }

    public void runCreateErrands(ServiceInstance instance, Plan plan, Deployment deployment,
                                 Observable<List<ErrandSummary>> errands) {}

    protected void runUpdateErrands(ServiceInstance instance, Plan plan, Deployment deployment,
                                    Observable<List<ErrandSummary>> errands) {}

    protected void runDeleteErrands(ServiceInstance instance, Deployment deployment, Observable<List<ErrandSummary>> errands) {}

    @Override
    protected void updateHosts(ServiceInstance instance, Plan plan, Deployment deployment) {

        List<Vm> vms = connection.connection()
                .vms().listDetails(BoshPlatformService.DEPLOYMENT_NAME_PREFIX + instance.getId())
                .toBlocking().first();

        if(instance.getHosts() == null) {
            instance.setHosts(new ArrayList<>());
        } else
            instance.getHosts().clear();

        vms.forEach(vm -> instance.getHosts().add(new ServerAddress("Host-" + vm.getIndex(), vm.getIps().get(0), defaultPort)));
    }

    @Override
    public void postDeleteInstance(ServiceInstance serviceInstance) {
        try {
            OpenstackConnectionFactory
                    .connection()
                    .compute()
                    .floatingIps()
                    .deallocateIP(serviceInstance.getFloatingIpId());
        } catch(Exception ex) {
            log.info("Could not deallocate IP on OS", ex);
        }
    }

}

package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.bean.OpenstackBean;
import de.evoila.cf.broker.bean.SiteConfiguration;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.EnvironmentUtils;
import de.evoila.cf.broker.model.GlobalConstants;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.plan.Plan;
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

import java.util.List;
import java.util.Optional;

/**
 * @author Rene Schollmeyer, Johannes Hiemer.
 */
@Service
@ConditionalOnBean({ BoshProperties.class, OpenstackBean.class })
public class LbaaSBoshPlatformService extends BoshPlatformService {

    private static final int defaultPort = 80;

    private Environment environment;

    public LbaaSBoshPlatformService(PlatformRepository repository, CatalogService catalogService,
                                    ServicePortAvailabilityVerifier availabilityVerifier,
                                    BoshProperties boshProperties,
                                    SiteConfiguration siteConfiguration,
                                    Optional<DashboardClient> dashboardClient,
                                    OpenstackBean openstackBean,
                                    Environment environment) {
        super(repository, catalogService,
                availabilityVerifier, boshProperties,
                dashboardClient, new LbaaSDeploymentManager(boshProperties, environment, siteConfiguration, openstackBean));
        this.environment = environment;
    }

    public void runCreateErrands(ServiceInstance instance, Plan plan, Deployment deployment,
                                 Observable<List<ErrandSummary>> errands) {}

    protected void runUpdateErrands(ServiceInstance instance, Plan plan, Deployment deployment,
                                    Observable<List<ErrandSummary>> errands) {}

    protected void runDeleteErrands(ServiceInstance instance, Deployment deployment, Observable<List<ErrandSummary>> errands) {}

    @Override
    protected void updateHosts(ServiceInstance serviceInstance, Plan plan, Deployment deployment) {
        List<Vm> vms = super.getVms(serviceInstance);
        serviceInstance.getHosts().clear();

        vms.forEach(vm -> serviceInstance.getHosts().add(super.toServerAddress(vm, defaultPort)));
    }

    @Override
    public void postDeleteInstance(ServiceInstance serviceInstance) throws PlatformException {
        try {
            deallocateFloatingIP(serviceInstance.getFloatingIpId());
        } catch(Exception ex) {
            throw new PlatformException("Could not deallocate Floating IP.", ex);
        }
    }

    private void deallocateFloatingIP(String floatingIpId) {
        if (EnvironmentUtils.isEnvironment(GlobalConstants.OPENSTACK_PROFILE, this.environment)) {
            OpenstackConnectionFactory
                    .connection()
                    .compute()
                    .floatingIps()
                    .deallocateIP(floatingIpId);
        } else if (EnvironmentUtils.isEnvironment(GlobalConstants.LOCAL_PROFILE, this.environment)) {
            log.info("Deallocation of Floating IP for profile 'local'. No need to do anything.");
        }
    }

}

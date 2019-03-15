package de.evoila.cf.broker.service.custom;

import de.evoila.cf.broker.model.RouteBinding;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.ServiceInstanceBinding;
import de.evoila.cf.broker.model.ServiceInstanceBindingRequest;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.repository.*;
import de.evoila.cf.broker.service.AsyncBindingService;
import de.evoila.cf.broker.service.HAProxyService;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Johannes Hiemer.
 */
@Service
public class LbaaSBindingService extends BindingServiceImpl {

	private Logger log = LoggerFactory.getLogger(getClass());

    public LbaaSBindingService(BindingRepository bindingRepository, ServiceDefinitionRepository serviceDefinitionRepository,
                               ServiceInstanceRepository serviceInstanceRepository, RouteBindingRepository routeBindingRepository,
                               HAProxyService haProxyService, JobRepository jobRepository,
                               AsyncBindingService asyncBindingService, PlatformRepository platformRepository) {
        super(bindingRepository, serviceDefinitionRepository, serviceInstanceRepository, routeBindingRepository,
                haProxyService, jobRepository, asyncBindingService, platformRepository);
    }

	@Override
	protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
		throw new UnsupportedOperationException();
	}

    @Override
    protected void unbindService(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) {}

    @Override
    protected Map<String, Object> createCredentials(String bindingId, ServiceInstanceBindingRequest serviceInstanceBindingRequest,
                                                    ServiceInstance serviceInstance, Plan plan, ServerAddress serverAddress) {
        return null;
    }

}

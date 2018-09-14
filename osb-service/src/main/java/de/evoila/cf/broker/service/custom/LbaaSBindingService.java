/**
 * 
 */
package de.evoila.cf.broker.service.custom;

import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.repository.BindingRepository;
import de.evoila.cf.broker.repository.RouteBindingRepository;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.service.HAProxyService;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class LbaaSBindingService extends BindingServiceImpl {

	private Logger log = LoggerFactory.getLogger(getClass());

	public LbaaSBindingService(BindingRepository bindingRepository, ServiceDefinitionRepository serviceDefinitionRepository, ServiceInstanceRepository serviceInstanceRepository,
							   RouteBindingRepository routeBindingRepository, HAProxyService haProxyService) {
		super(bindingRepository, serviceDefinitionRepository, serviceInstanceRepository, routeBindingRepository, haProxyService);
	}

	@Override
	protected Map<String, Object> createCredentials(String bindingId, ServiceInstanceBindingRequest serviceInstanceBindingRequest,
                                                    ServiceInstance serviceInstance, Plan plan, ServerAddress host) {
        throw new UnsupportedOperationException();
	}

	@Override
	protected void unbindService(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) {
	    throw new UnsupportedOperationException();
	}

	@Override
	public ServiceInstanceBinding getServiceInstanceBinding(String id) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
		throw new UnsupportedOperationException();
	}

}

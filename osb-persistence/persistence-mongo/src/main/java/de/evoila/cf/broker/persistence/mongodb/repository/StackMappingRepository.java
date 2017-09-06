package de.evoila.cf.broker.persistence.mongodb.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by reneschollmeyer, evoila on 25.08.17.
 */
@ConditionalOnProperty(prefix = "openstack", name = { "endpoint" }, havingValue = "")
public interface StackMappingRepository extends MongoRepository<ServiceStackMapping, String> {

}

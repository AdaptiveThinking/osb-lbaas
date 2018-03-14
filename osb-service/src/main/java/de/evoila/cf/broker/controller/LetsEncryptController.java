package de.evoila.cf.broker.controller;

import com.google.common.base.Splitter;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.NsLookupRequest;
import de.evoila.cf.broker.model.NsLookupResponse;
import de.evoila.cf.cpi.bosh.LbaaSBoshPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by reneschollmeyer, evoila on 13.03.18.
 */
@RestController
@RequestMapping(value = "v2")
public class LetsEncryptController  {
    private final Logger log = LoggerFactory.getLogger(LetsEncryptController.class);

    @Autowired
    private LbaaSBoshPlatformService lbaaSBoshPlatformService;

    @PostMapping(value = "manage/service_instances/{instanceId}/dns", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity letsEncrypt(@PathVariable("instanceId") String instanceId,
                                      @RequestBody NsLookupRequest request) throws ServiceInstanceDoesNotExistException {

        List<String> domainList = Splitter.on(",").splitToList(request.getDomains());

        //TODO: Welche IP und woher?
        String fip = "";

        //log.info("Domains: " + domainList.toString());
        //log.info("Public IP: " + fip);

        NsLookupResponse response = new NsLookupResponse();

        for(String domain : domainList) {
            if(!nslookup(domain, fip)) {
                response.getFalseResults().get("message").add(domain);
            }
        }

        if(response.getFalseResults().get("message").isEmpty()) {
            return new ResponseEntity<>("{ \"message\": \"OK\"}", HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response.getFalseResults(), HttpStatus.ACCEPTED);
        }
    }

    private boolean nslookup(String host, String ip) {
        try {
            if(InetAddress.getByName(host.trim()).getHostAddress().equals(ip)) {
                return true;
            }
        } catch (UnknownHostException e) {
            return false;
        }

        return false;
    }
}

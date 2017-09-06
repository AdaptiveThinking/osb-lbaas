package de.evoila.cf.broker.service.availabilty;

import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.service.ServiceInstanceAvailabilityVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Created by reneschollmeyer, evoila on 05.09.17.
 */
@Service
@Primary
public class SSLServiceAvailabilityVerifier implements ServiceInstanceAvailabilityVerifier {

    private static final int INITIAL_TIMEOUT = 150 * 1000;

    private static final int connectionTimeouts = 10;

    private final Logger log = LoggerFactory.getLogger(SSLServiceAvailabilityVerifier.class);

    private RestTemplate restTemplate;

    @PostConstruct
    private void init() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        restTemplate = restTemplate();

        restTemplate.setErrorHandler(new ResponseErrorHandler() {

            @Override
            public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {
                log.warn(clientHttpResponse.getStatusText());
            }
        });
    }

    private RestTemplate restTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;
    }

    private void timeout(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            log.info("Starting new timeout interval was interrupted.", e);
        }
    }

    private boolean execute(String ip, int port) {
        log.info("Verifying port availability on: {}:{}", ip, port);
        URI uri = URI.create("https://" + ip + ":" + port);

        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

        if(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError()) {
            return true;
        }

        return false;
    }

    private boolean verifyServiceAvailability(String ip, int port, boolean useInitialTimeout) throws PlatformException {
        boolean available = false;

        if(useInitialTimeout)
            this.timeout(INITIAL_TIMEOUT);

        for(int i = 0; i < connectionTimeouts; i++) {
            available = this.execute(ip, port);

            log.info("Service Port availability: {}", available);

            if(available) {
                break;
            }
        }

        log.info("Service Port availability (last status during request): {}", available);

        return available;
    }

    @Override
    public boolean verifyServiceAvailability(ServiceInstance serviceInstance, boolean useInitialTimeout) throws PlatformException {
        List<ServerAddress> serverAddresses = serviceInstance.getHosts();

        for(ServerAddress serverAddress : serverAddresses) {
            if(!verifyServiceAvailability(serverAddress.getIp(), serverAddress.getPort(), useInitialTimeout)) {
                return false;
            }
        }

        return true;
    }
}

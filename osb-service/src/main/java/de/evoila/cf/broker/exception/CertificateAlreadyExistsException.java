package de.evoila.cf.broker.exception;

/**
 * Created by reneschollmeyer, evoila on 26.09.17.
 */
public class CertificateAlreadyExistsException extends Exception {

    private String serviceInstanceId;

    public CertificateAlreadyExistsException(String serviceInstanceId) { this.serviceInstanceId = serviceInstanceId; }

    @Override
    public String getMessage() { return "ServiceInstance already has a certificate: id = " + serviceInstanceId; }
}

package de.evoila.cf.broker.bean.impl;

import de.evoila.cf.broker.bean.LbaaSBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by reneschollmeyer, evoila on 30.08.17.
 */
@Service
@ConfigurationProperties(prefix = "lbaas")
public class LbaaSBeanImpl implements LbaaSBean {

    private int port;
    private List<String> addresses;

    public void setPort(int port) { this.port = port; }

    public void setAddresses(List<String> addresses) { this.addresses  = addresses; }

    @Override
    public int getPort() { return port; }

    @Override
    public List<String> getAddresses() { return addresses; }
}

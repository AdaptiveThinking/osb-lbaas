package de.evoila.cf.broker.bean;

import java.util.List;

/**
 * Created by reneschollmeyer, evoila on 30.08.17.
 */
public interface LbaaSBean {
    public int getPort();
    public List<String> getAddresses();
}

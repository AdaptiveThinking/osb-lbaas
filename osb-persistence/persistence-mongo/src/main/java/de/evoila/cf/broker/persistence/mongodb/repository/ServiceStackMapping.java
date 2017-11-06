package de.evoila.cf.broker.persistence.mongodb.repository;

import de.evoila.cf.broker.model.BaseEntity;

/**
 * Created by reneschollmeyer, evoila on 25.08.17.
 */
public class ServiceStackMapping implements BaseEntity<String>{
    private String id;
    private String lbaasStack;
    private boolean certified;

    @Override
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getLbaasStack() { return lbaasStack; }

    public void setLbaasStack(String lbaasStack) { this.lbaasStack = lbaasStack; }

    public boolean getCertified() { return certified; }

    public void setCertified(boolean certified) { this.certified = certified; }
}

package com.storeops.programmes.model;

import java.util.ArrayList;
import java.util.List;

/** A store programme (seasonal rollout, compliance drive, refit). */
public class Project {
    private String id;
    private String name;
    private String storeId;
    private String ownerId;
    private final List<ProjectMember> members = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public List<ProjectMember> getMembers() { return members; }
}

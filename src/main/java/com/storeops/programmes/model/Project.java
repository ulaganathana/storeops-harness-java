package com.storeops.programmes.model;

import java.util.ArrayList;
import java.util.List;

public record Project(String id, String name, String description, String storeId, List<ProjectMember> members) {

    public boolean hasMember(String staffId) {
        return members.stream().anyMatch(member -> member.staffId().equals(staffId));
    }

    public Project withMember(ProjectMember member) {
        List<ProjectMember> updated = new ArrayList<>(members);
        updated.add(member);
        return new Project(id, name, description, storeId, List.copyOf(updated));
    }
}

package com.storeops.programmes.repository;

import com.storeops.programmes.model.Project;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProgrammeRepository implements ProgrammeRepository {
    private final Map<String, Project> store = new ConcurrentHashMap<>();

    @Override
    public List<Project> findByStoreId(String storeId) {
        return store.values().stream().filter(p -> p.getStoreId().equals(storeId)).toList();
    }

    @Override
    public Optional<Project> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Project save(Project project) {
        if (project.getId() == null) {
            project.setId(UUID.randomUUID().toString());
        }
        store.put(project.getId(), project);
        return project;
    }
}

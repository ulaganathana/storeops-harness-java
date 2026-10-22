package com.storeops.programmes.repository;

import com.storeops.programmes.model.Project;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class ProgrammeRepository {

    private final Map<String, Project> programmes = new ConcurrentHashMap<>();

    public List<Project> findByStoreId(String storeId) {
        return programmes.values().stream()
                .filter(programme -> programme.storeId().equals(storeId))
                .sorted(Comparator.comparing(Project::name).thenComparing(Project::id))
                .toList();
    }

    public Optional<Project> findById(String id) {
        return Optional.ofNullable(programmes.get(id));
    }

    public Project save(Project programme) {
        programmes.put(programme.id(), programme);
        return programme;
    }
}

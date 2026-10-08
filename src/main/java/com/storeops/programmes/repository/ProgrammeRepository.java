package com.storeops.programmes.repository;

import com.storeops.programmes.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProgrammeRepository {
    List<Project> findByStoreId(String storeId);

    Optional<Project> findById(String id);

    Project save(Project project);
}

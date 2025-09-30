package org.example.schemaflow.shared.infrastucture.repositories;

import org.example.schemaflow.shared.domain.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
}

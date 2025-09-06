package org.example.schemaflow.shared.infrastucture.repositories;

import org.example.schemaflow.shared.domain.entities.Role;
import org.example.schemaflow.shared.domain.entities.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
package org.example.schemaflow.shared.infrastucture.repositories;

import org.example.schemaflow.shared.domain.entities.Role;
import org.example.schemaflow.shared.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    boolean existsByRoles(List<Role> roles);
}

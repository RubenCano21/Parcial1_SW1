package org.example.schemaflow.shared.application.service;

import org.example.schemaflow.shared.domain.dto.RoleDTO;
import org.example.schemaflow.shared.domain.dto.RoleResponseDTO;
import org.example.schemaflow.shared.domain.entities.Role;

import java.util.List;
import java.util.Optional;

public interface RoleService {

    List<Role> getAllRoles();

    Optional<Role> getRoleById(Long id);

    RoleResponseDTO createRole(RoleDTO roleDTO);

    Role updateRole(Long id, Role updateRole);

    void deleteRole(Long id);
}

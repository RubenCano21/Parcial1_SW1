package org.example.schemaflow.shared.application.service;

import org.example.schemaflow.shared.domain.dto.UserDTO;
import org.example.schemaflow.shared.domain.dto.UserRoleRequest;
import org.example.schemaflow.shared.domain.entities.User;

import java.util.List;

public interface UserService {

    List<User> findAll();

    User save(User user);

    boolean existsByUsername(String username);

    User update(Long id, User user);

    User asignRole(UserRoleRequest request);

    User removeRole(UserRoleRequest request);


    void delete(Long id);

    UserDTO findById(Long id);
}
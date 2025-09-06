package org.example.schemaflow.shared.application.service;

import lombok.RequiredArgsConstructor;
import org.example.schemaflow.shared.domain.dto.UserDTO;
import org.example.schemaflow.shared.domain.dto.UserRoleRequest;
import org.example.schemaflow.shared.domain.entities.Role;
import org.example.schemaflow.shared.domain.entities.RoleName;
import org.example.schemaflow.shared.domain.entities.User;
import org.example.schemaflow.shared.domain.mapper.UserMapper;
import org.example.schemaflow.shared.infrastucture.repositories.RoleRepository;
import org.example.schemaflow.shared.infrastucture.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;


    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return  repository.findAll();
    }

    @Override
    @Transactional
    public User save(User user) {

        Optional<Role> optionalRoleUser = roleRepository.findByName(RoleName.ROLE_USER);
        List<Role> roles = new ArrayList<>();

        optionalRoleUser.ifPresent(roles::add);

        if (user.isAdmin()) {
            Optional<Role> optionalRoleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN);
            optionalRoleAdmin.ifPresent(roles::add);
        }

        user.setRoles(roles);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return repository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public User update(Long id, User user) {
        Optional<User> optionalUser = repository.findById(id);
        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();
            existingUser.setUsername(user.getUsername());
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            existingUser.setAdmin(user.isAdmin());
            return repository.save(existingUser);
        }
        return null;
    }

    @Override
    public User asignRole(UserRoleRequest request) {
        User user = repository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User no encontrado"));

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role no encontrado"));

        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            repository.save(user);
        }
        return user;
    }

    @Override
    public User removeRole(UserRoleRequest request) {
        User user = repository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User no encontrado"));

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role no encontrado"));

        if (!user.getRoles().contains(role)) {
            user.getRoles().remove(role);
            repository.save(user);
        }
        return user;
    }

    @Override
    public void delete(Long id) {

        Optional<User> optionalUser = repository.findById(id);
        optionalUser.ifPresent(repository::delete);
    }

    @Override
    public UserDTO findById(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User no encontrado"));

        return userMapper.toDto(user);
    }


}

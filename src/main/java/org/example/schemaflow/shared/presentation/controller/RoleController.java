package org.example.schemaflow.shared.presentation.controller;

import org.example.schemaflow.shared.application.service.RoleService;
import org.example.schemaflow.shared.domain.dto.RoleDTO;
import org.example.schemaflow.shared.domain.dto.RoleResponseDTO;
import org.example.schemaflow.shared.domain.entities.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/list")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<RoleResponseDTO> createRole(@RequestBody RoleDTO roleDTO) {
        RoleResponseDTO createdRole = roleService.createRole(roleDTO);
        return ResponseEntity.status(201).body(createdRole);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        return ResponseEntity.ok(roleService.updateRole(id, role));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}

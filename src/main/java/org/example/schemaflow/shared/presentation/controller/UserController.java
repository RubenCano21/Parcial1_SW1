package org.example.schemaflow.shared.presentation.controller;

import jakarta.validation.Valid;
import org.example.schemaflow.shared.application.service.UserService;
import org.example.schemaflow.shared.domain.dto.UserDTO;
import org.example.schemaflow.shared.domain.dto.UserRoleRequest;
import org.example.schemaflow.shared.domain.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins="http://localhost:4200", originPatterns = "*")
@RestController
@RequestMapping("/api/users")
public class UserController {


    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public List<User> list() {
        return userService.findAll();
    }

    //@PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody User user, BindingResult result) {
        if (result.hasFieldErrors()) {
            return validation(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.save(user));
    }

    //@PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user, BindingResult result) {
        //user.setAdmin(false);
        return create(user, result);
    }

    private ResponseEntity<?> validation(BindingResult result) {
        Map<String, String> errors = new HashMap<>();

        result.getFieldErrors().forEach(err -> {
            errors.put(err.getField(), "El campo " + err.getField() + " " + err.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @PutMapping("/update-user/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody User user, BindingResult result) {
        if (result.hasFieldErrors()) {
            return validation(result);
        }
        return ResponseEntity.status(HttpStatus.OK).body(userService.update(id, user));
    }

    @DeleteMapping("/delete-user/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body("Usuario eliminado");
    }


    @PostMapping("/assign-role")
    public ResponseEntity<User> assignRole(@RequestBody UserRoleRequest request) {
        return ResponseEntity.ok(userService.asignRole(request));
    }

    @PostMapping("/remove-role")
    public ResponseEntity<User> removeRole(@RequestBody UserRoleRequest request) {
        return ResponseEntity.ok(userService.removeRole(request));
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<UserDTO> findById(@PathVariable Long id) {

        UserDTO userDTO = userService.findById(id);
        return ResponseEntity.ok(userDTO);
    }

}

